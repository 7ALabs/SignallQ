import argparse
import dataclasses
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def _safe_filename(url_path: str) -> str:
    url_path = url_path.lstrip("/")
    if not url_path:
        return "root"
    return re.sub(r"[^A-Za-z0-9._-]+", "_", url_path)


def _write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", errors="replace")


def _write_bytes(path: Path, content: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)


def _dump_headers(resp: requests.Response) -> str:
    lines = [f"HTTP/{resp.raw.version/10:.1f} {resp.status_code} {resp.reason}"]
    for k, v in resp.headers.items():
        lines.append(f"{k}: {v}")
    lines.append("")
    return "\n".join(lines)


def _extract_title(html: str) -> str | None:
    m = re.search(r"<title[^>]*>(.*?)</title>", html, flags=re.IGNORECASE | re.DOTALL)
    if not m:
        return None
    return re.sub(r"\s+", " ", m.group(1)).strip()


def _extract_assets(html: str) -> list[str]:
    assets: set[str] = set()
    for attr in ("src", "href"):
        for m in re.finditer(
            rf"""{attr}\s*=\s*(["'])(.+?)\1""",
            html,
            flags=re.IGNORECASE | re.DOTALL,
        ):
            val = m.group(2).strip()
            if not val:
                continue
            if val.startswith(("http://", "https://", "data:", "blob:")):
                continue
            # Ignore anchors / JS pseudo-urls
            if val.startswith(("#", "javascript:")):
                continue
            assets.add(val)
    return sorted(assets)


def _extract_login_hints(html: str) -> dict:
    out: dict[str, object] = {}

    # JS-embedded vars (commonly dynamic per page load)
    m = re.search(r'var\s+nonce\s*=\s*"([^"]+)"', html)
    if m:
        out["nonce"] = m.group(1)
    m = re.search(r'var\s+token\s*=\s*"([^"]+)"', html)
    if m:
        out["csrf_token"] = m.group(1)

    # Public key literal (JS string with line-continuations via backslash)
    m = re.search(r"var\s+pubkey\s*=\s*'(.+?)';", html, flags=re.DOTALL)
    if m:
        raw = m.group(1)
        out["pubkey_js_literal"] = raw
        out["pubkey_effective"] = raw.replace("\\\r\n", "").replace("\\\n", "")

    # Login endpoint
    m = re.search(r"url\s*:\s*['\"](/[^'\"]*login\.cgi)['\"]", html)
    if m:
        out["login_url"] = m.group(1)
    elif "login.cgi" in html:
        out["login_url"] = "/login.cgi"

    # Inputs (best-effort; HTML is often messy)
    inputs: list[dict[str, str]] = []
    for m in re.finditer(r"<input\b([^>]+)>", html, flags=re.IGNORECASE):
        attrs = m.group(1)
        d: dict[str, str] = {}
        for am in re.finditer(r"""(\w+)\s*=\s*(["'])(.*?)\2""", attrs):
            d[am.group(1).lower()] = am.group(3)
        if d:
            inputs.append(d)
    if inputs:
        out["inputs"] = inputs

    # Look for AJAX POST + success markers
    out["mentions_http_299"] = "status == 299" in html or "== 299" in html
    out["mentions_x_sid"] = "X-SID" in html
    out["mentions_localstorage"] = "localStorage" in html

    return out


@dataclasses.dataclass
class FetchResult:
    url: str
    status_code: int
    headers_path: str
    body_path: str
    location: str | None = None
    title: str | None = None


def _fetch(session: requests.Session, url: str, out_dir: Path, name_prefix: str) -> FetchResult:
    resp = session.get(url, allow_redirects=False, timeout=15)
    headers_path = out_dir / f"{name_prefix}_headers.txt"
    body_path = out_dir / f"{name_prefix}.html"
    _write_text(headers_path, _dump_headers(resp))
    _write_bytes(body_path, resp.content)

    title = None
    ctype = resp.headers.get("Content-Type", "")
    if "text/html" in ctype.lower():
        title = _extract_title(resp.text)

    return FetchResult(
        url=url,
        status_code=resp.status_code,
        headers_path=str(headers_path),
        body_path=str(body_path),
        location=resp.headers.get("Location"),
        title=title,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Reconhecimento controlado do modem (GET-only).")
    parser.add_argument("--host", default="http://192.168.1.254", help="Base URL do modem (default: %(default)s)")
    parser.add_argument("--out", default="", help="Diretório de saída (default: source/modem_probe_<data>)")
    parser.add_argument("--download-assets", action="store_true", help="Baixar JS/CSS referenciados pela página inicial")
    args = parser.parse_args()

    host = args.host.rstrip("/")
    if not host.startswith(("http://", "https://")):
        print("ERRO: --host precisa incluir http:// ou https://", file=sys.stderr)
        return 2

    if args.out:
        out_dir = Path(args.out)
    else:
        out_dir = Path("source") / f"modem_probe_{datetime.now().date().isoformat()}"
    out_dir.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    session.headers.update(
        {
            "User-Agent": "LINKA-modem-probe/1.0 (phase1; GET-only)",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        }
    )

    results: dict[str, object] = {
        "generated_at_utc": _now_iso(),
        "host": host,
        "phase": 1,
        "notes": [
            "GET-only. Não tenta autenticar. Não executa POST/PUT/etc.",
            "Use --download-assets para baixar JS/CSS referenciados no HTML.",
        ],
        "fetches": [],
        "session_cookies_after_root_get": {},
        "assets": [],
        "login_hints": {},
    }

    # Fetch root without following redirects; record chain manually
    chain: list[FetchResult] = []
    url = host + "/"
    for i in range(0, 8):
        fr = _fetch(session, url, out_dir, name_prefix=f"step{i}_root")
        chain.append(fr)
        if fr.status_code in (301, 302, 303, 307, 308):
            if not fr.location:
                break
            url = urljoin(url, fr.location)
            continue
        break

    for fr in chain:
        results["fetches"].append(dataclasses.asdict(fr))

    results["session_cookies_after_root_get"] = session.cookies.get_dict()

    # Parse last HTML (if any) for assets + login hints
    last = chain[-1]
    html = Path(last.body_path).read_text(encoding="utf-8", errors="replace")
    results["login_hints"] = _extract_login_hints(html)
    assets = _extract_assets(html)
    results["assets"] = assets

    if args.download_assets:
        assets_dir = out_dir / "assets"
        fetched_assets: list[dict[str, object]] = []
        for a in assets:
            # Normalize to absolute URL under host
            if a.startswith("/"):
                a_url = host + a
                rel_key = a
            else:
                a_url = urljoin(host + "/", a)
                rel_key = "/" + a.lstrip("./")

            parsed = urlparse(a_url)
            if parsed.scheme not in ("http", "https"):
                continue

            fname = _safe_filename(rel_key)
            out_path = assets_dir / fname
            hdr_path = assets_dir / f"{fname}.headers.txt"
            try:
                r = session.get(a_url, allow_redirects=False, timeout=15)
                _write_text(hdr_path, _dump_headers(r))
                _write_bytes(out_path, r.content)
                fetched_assets.append(
                    {
                        "url": a_url,
                        "status_code": r.status_code,
                        "saved_as": str(out_path),
                        "headers_saved_as": str(hdr_path),
                    }
                )
            except requests.RequestException as e:
                fetched_assets.append({"url": a_url, "error": str(e)})

        results["assets_fetched"] = fetched_assets

    summary_path = out_dir / "probe_summary.json"
    _write_text(summary_path, json.dumps(results, indent=2, ensure_ascii=False))

    print(f"[OK] Wrote {summary_path}")
    if last.title:
        print(f"[Root Title] {last.title}")
    login_url = results.get("login_hints", {}).get("login_url")
    if login_url:
        print(f"[Login Endpoint Hint] {login_url}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
