import argparse
import base64
import hashlib
import json
import os
import re
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urljoin
from urllib.parse import quote
from urllib.parse import urlencode

import requests
from Crypto.Cipher import AES, PKCS1_v1_5
from Crypto.PublicKey import RSA


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


@dataclass
class FormSummary:
    page: str
    action: str
    csrf_token: str | None
    has_wl_enable: bool
    hidden_fields: dict[str, str]
    all_field_names: list[str]
    default_radio_enabled: int | None
    default_ssid_enable: int | None
    default_ssid_broadcast: int | None
    default_channel: str | None
    default_ssid: str | None
    field_count: int


@dataclass
class ToggleEvidence:
    band: str
    requested_enabled: bool
    request_url: str
    request_fields_count: int
    response_status: int
    response_len: int
    response_preview: str | None
    confirmed_enabled: bool | None
    checks: int
    error_code: str | None = None
    error_detail: str | None = None


def _extract_first_form(html: str) -> tuple[str, str] | None:
    match = re.search(
        r"<form[^>]*action=[\"']([^\"']+)[\"'][^>]*>([\s\S]*?)</form>",
        html,
        flags=re.IGNORECASE,
    )
    if not match:
        return None
    return match.group(1).strip(), match.group(2)


def _extract_input_fields(form_html: str) -> dict[str, str]:
    fields: dict[str, str] = {}
    for match in re.finditer(r"<input\b([^>]*)>", form_html, flags=re.IGNORECASE):
        attrs = match.group(1)
        name_match = re.search(r"name=[\"']([^\"']+)[\"']", attrs, flags=re.IGNORECASE)
        if not name_match:
            continue
        name = name_match.group(1).strip()
        value_match = re.search(
            r"value=[\"']([^\"']*)[\"']",
            attrs,
            flags=re.IGNORECASE,
        )
        value = value_match.group(1) if value_match else ""
        fields[name] = value
    return fields


def _extract_form_serialized_defaults(form_html: str) -> dict[str, str]:
    fields: dict[str, str] = {}
    for match in re.finditer(r"<input\b([^>]*)>", form_html, flags=re.IGNORECASE):
        attrs = match.group(1)
        name_match = re.search(r"name=[\"']([^\"']+)[\"']", attrs, flags=re.IGNORECASE)
        if not name_match:
            continue
        name = name_match.group(1).strip()
        type_match = re.search(r"type=[\"']([^\"']+)[\"']", attrs, flags=re.IGNORECASE)
        input_type = (type_match.group(1).strip().lower() if type_match else "text")
        value_match = re.search(r"value=[\"']([^\"']*)[\"']", attrs, flags=re.IGNORECASE)
        value = value_match.group(1) if value_match else ""

        if input_type == "checkbox":
            if re.search(r"\bchecked\b", attrs, flags=re.IGNORECASE):
                fields[name] = value or "on"
            continue
        fields[name] = value

    fields.update(_extract_select_defaults(form_html))
    return fields


def _extract_select_defaults(form_html: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for select in re.finditer(
        r"<select\b([^>]*)>([\s\S]*?)</select>",
        form_html,
        flags=re.IGNORECASE,
    ):
        attrs = select.group(1)
        body = select.group(2)
        name_match = re.search(r"name=[\"']([^\"']+)[\"']", attrs, flags=re.IGNORECASE)
        if not name_match:
            continue
        name = name_match.group(1).strip()
        selected = re.search(
            r"<option[^>]*selected[^>]*value=[\"']([^\"']+)[\"']",
            body,
            flags=re.IGNORECASE,
        )
        if selected:
            out[name] = selected.group(1)
            continue
        first_option = re.search(
            r"<option[^>]*value=[\"']([^\"']+)[\"']",
            body,
            flags=re.IGNORECASE,
        )
        if first_option:
            out[name] = first_option.group(1)
    return out


def _extract_csrf_token(html: str) -> str | None:
    token_from_input = re.search(
        r'name=["\']csrf_token["\'][^>]*value=["\']([^"\']+)["\']',
        html,
        flags=re.IGNORECASE,
    )
    if token_from_input:
        return token_from_input.group(1)
    token_from_js = re.search(r"csrf_token=([A-Za-z0-9]+)", html)
    if token_from_js:
        return token_from_js.group(1)
    return None


def _extract_radio_enabled(html: str) -> int | None:
    matches = re.findall(r"RadioEnabled\s*:\s*([01])", html)
    if not matches:
        return None
    return int(matches[0])


def _extract_primary_wlan_block(html: str, band: str) -> str | None:
    target_id = "1" if band == "2.4g" else "5"
    m = re.search(rf"\b{target_id}\s*:\s*\{{", html)
    if not m:
        return None
    start_brace = html.find("{", m.start())
    if start_brace < 0:
        return None
    depth = 0
    for idx in range(start_brace, len(html)):
        ch = html[idx]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return html[start_brace : idx + 1]
    return None


def _extract_numeric_from_block(block: str | None, key: str) -> int | None:
    if not block:
        return None
    match = re.search(rf"{re.escape(key)}\s*:\s*([0-9]+)", block)
    return int(match.group(1)) if match else None


def _extract_string_from_block(block: str | None, key: str) -> str | None:
    if not block:
        return None
    match = re.search(rf"{re.escape(key)}\s*:\s*'([^']*)'", block)
    return match.group(1) if match else None


def _extract_ssid(html: str) -> str | None:
    match = re.search(r"SSID\s*:\s*'([^']+)'", html)
    if not match:
        return None
    return match.group(1)


def summarize_html(page: str, html: str) -> FormSummary:
    parsed = _extract_first_form(html)
    if parsed is None:
        return FormSummary(
            page=page,
            action="",
            csrf_token=_extract_csrf_token(html),
            has_wl_enable=False,
            hidden_fields={},
            all_field_names=[],
            default_radio_enabled=_extract_radio_enabled(html),
            default_ssid=_extract_ssid(html),
            field_count=0,
        )
    action, form_body = parsed
    input_fields = _extract_input_fields(form_body)
    serialize_defaults = _extract_form_serialized_defaults(form_body)
    select_defaults = _extract_select_defaults(form_body)
    all_fields = {**input_fields, **select_defaults}
    wlan_block = _extract_primary_wlan_block(html, page)

    hidden_fields: dict[str, str] = {}
    for name, value in input_fields.items():
        if name.startswith("wl_") or name.startswith("csrf_"):
            hidden_fields[name] = value

    if wlan_block:
        key_map_numeric = {
            "WMMEnable": "wl_wmm",
            "X_ASB_COM_BaseCfg_GlobalMaxAssoc": "total_max_user",
            "X_ASB_COM_VirtualIfCfg_MaxAssoc": "max_user",
            "WPSEnable": "wl_wps",
        }
        for source_key, target_field in key_map_numeric.items():
            value = _extract_numeric_from_block(wlan_block, source_key)
            if value is not None:
                hidden_fields[target_field] = str(value)

        wps_mode = _extract_string_from_block(wlan_block, "WPSMode")
        if wps_mode is not None:
            hidden_fields["wl_wpsmode"] = wps_mode

        standard = _extract_string_from_block(wlan_block, "Standard")
        if standard is not None:
            hidden_fields["wl_mode"] = standard

        beacon_type = _extract_string_from_block(wlan_block, "BeaconType")
        if beacon_type is not None:
            if page == "2.4g":
                if beacon_type == "WPAand11i":
                    hidden_fields["wl_encryptmode"] = "Private"
                    hidden_fields["wl_wpaver"] = "WPAand11i"
                elif beacon_type == "11i":
                    hidden_fields["wl_encryptmode"] = "Private1"
                    hidden_fields["wl_wpaver"] = "11i"
            else:
                if beacon_type == "11i":
                    hidden_fields["wl_encryptmode"] = "Private1"
                elif beacon_type == "WPAand11i":
                    hidden_fields["wl_encryptmode"] = "Private2"

        wpa_enc = _extract_string_from_block(wlan_block, "WPAEncryptionModes")
        if wpa_enc is not None:
            hidden_fields["wpaenc"] = wpa_enc

    return FormSummary(
        page=page,
        action=action,
        csrf_token=_extract_csrf_token(html),
        has_wl_enable="wl_enable" in all_fields,
        hidden_fields={**serialize_defaults, **hidden_fields},
        all_field_names=sorted(all_fields.keys()),
        default_radio_enabled=_extract_numeric_from_block(wlan_block, "RadioEnabled")
        if wlan_block
        else _extract_radio_enabled(html),
        default_ssid_enable=_extract_numeric_from_block(wlan_block, "Enable"),
        default_ssid_broadcast=_extract_numeric_from_block(wlan_block, "SSIDAdvertisementEnabled"),
        default_channel=(
            "Auto"
            if _extract_numeric_from_block(wlan_block, "AutoChannelEnable") == 1
            else str(_extract_numeric_from_block(wlan_block, "Channel"))
            if _extract_numeric_from_block(wlan_block, "Channel") is not None
            else None
        ),
        default_ssid=(
            _extract_string_from_block(wlan_block, "SSID")
            if wlan_block
            else _extract_ssid(html)
        ),
        field_count=len(serialize_defaults),
    )


def _build_toggle_payload(summary: FormSummary, enabled: bool) -> dict[str, str]:
    payload = dict(summary.hidden_fields)
    if summary.csrf_token:
        payload["csrf_token"] = summary.csrf_token
    if enabled:
        payload["wl_enable"] = "on"
    else:
        payload.pop("wl_enable", None)
    if summary.page == "2.4g":
        payload["wl_id"] = payload.get("wl_id", "1")
    elif summary.page == "5g":
        payload["wl_id"] = payload.get("wl_id", "5")
    if summary.default_ssid is not None:
        payload["wl_ssid"] = summary.default_ssid
        payload["wl_ssidname"] = summary.default_ssid
    if summary.default_ssid_enable is not None:
        payload["wl_en"] = str(summary.default_ssid_enable)
    if summary.default_ssid_broadcast is not None:
        payload["wl_broad"] = str(summary.default_ssid_broadcast)
    if summary.default_channel is not None:
        payload["wl_channel"] = summary.default_channel
    if not enabled:
        payload["wl_en"] = "0"
    if not payload.get("total_max_user"):
        payload["total_max_user"] = "32"
    if not payload.get("max_user"):
        payload["max_user"] = "32"
    payload["isReboot"] = payload.get("isReboot", "0")
    return payload


def _band_path(band: str) -> str:
    return "/wlan_config.cgi?v=11ac" if band == "5g" else "/wlan_config.cgi"


def _confirm_enabled(session: requests.Session, host: str, band: str) -> bool | None:
    url = host.rstrip("/") + _band_path(band)
    response = session.get(url, timeout=10)
    if response.status_code >= 400:
        return None
    value = _extract_radio_enabled(response.text)
    if value is None:
        return None
    return value == 1


def run_offline(args: argparse.Namespace) -> int:
    html24 = Path(args.input24).read_text(encoding="utf-8", errors="replace")
    html5 = Path(args.input5).read_text(encoding="utf-8", errors="replace")
    summary24 = summarize_html("2.4g", html24)
    summary5 = summarize_html("5g", html5)

    payloads = {
        "2.4g_on_min": _build_toggle_payload(summary24, True),
        "2.4g_off_min": _build_toggle_payload(summary24, False),
        "5g_on_min": _build_toggle_payload(summary5, True),
        "5g_off_min": _build_toggle_payload(summary5, False),
    }

    output = {
        "generated_at_utc": _now_iso(),
        "mode": "offline",
        "forms": {
            "2.4g": asdict(summary24),
            "5g": asdict(summary5),
        },
        "payload_min_contract": payloads,
        "error_codes": [
            "form_not_found",
            "post_failed",
            "confirmation_timeout",
            "session_invalid",
        ],
        "global_sequence": {
            "strategy": "sequential_by_band_no_rollback",
            "bands": ["2.4g", "5g"],
            "aggregation": "success|partial|failed",
        },
    }

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(output, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"[OK] offline contract: {out_path}")
    return 0


def _prepare_session(cookie_header: str | None) -> requests.Session:
    session = requests.Session()
    session.headers.update(
        {
            "User-Agent": "LINKA-wifi-toggle-reverse/1.0",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        }
    )
    if cookie_header:
        session.headers["Cookie"] = cookie_header
    return session


def _base64_url_escape(value: str) -> str:
    return value.replace("+", "-").replace("/", "_").replace("=", ".")


def _base64_url_no_pad(value: str) -> str:
    return value.replace("+", "-").replace("/", "_").replace("=", "")


def _extract_pubkey_base64(html: str) -> str | None:
    match = re.search(r"var\s+pubkey\s*=\s*'([^']+)'", html, flags=re.MULTILINE)
    if not match:
        return None
    pem_key = match.group(1).replace("\\\r\n", "").replace("\\\n", "")
    if "BEGIN PUBLIC KEY" not in pem_key or "END PUBLIC KEY" not in pem_key:
        return None
    payload = (
        pem_key.replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\r", "")
        .replace("\n", "")
        .strip()
    )
    return payload if payload else None


def _iso7816_pad(payload: bytes, block_size: int = 16) -> bytes:
    pad_len = block_size - (len(payload) % block_size)
    out = bytearray(payload)
    out.append(0x80)
    out.extend(b"\x00" * (pad_len - 1))
    return bytes(out)


def _aes_cbc_encrypt_sjcl(aes_key: bytes, iv: bytes, plaintext: bytes) -> str:
    cipher = AES.new(aes_key, AES.MODE_CBC, iv)
    ct = cipher.encrypt(_iso7816_pad(plaintext))
    return _base64_url_no_pad(base64.b64encode(ct).decode())


def _sha256_b64(text: str) -> str:
    return base64.b64encode(hashlib.sha256(text.encode("utf-8")).digest()).decode()


def _build_login_plain_payload(
    username: str,
    password: str,
    nonce: str,
    csrf_token: str,
    dec_key: bytes,
    dec_iv: bytes,
) -> str:
    return (
        f"&username={username}"
        f"&password={quote(password, safe='')}"
        f"&csrf_token={csrf_token}"
        f"&nonce={nonce}"
        f"&enckey={_base64_url_escape(base64.b64encode(dec_key).decode())}"
        f"&enciv={_base64_url_escape(base64.b64encode(dec_iv).decode())}"
    )


def _build_login_digest_payload(
    username: str,
    password: str,
    nonce: str,
    csrf_token: str,
    dec_key: bytes,
    dec_iv: bytes,
) -> str:
    a1 = _sha256_b64(f"{username}:{password}")
    response = _base64_url_escape(_sha256_b64(f"{a1}:{nonce}"))
    userhash = _base64_url_escape(_sha256_b64(f"{username}:{nonce}"))
    return (
        f"userhash={userhash}"
        f"&response={response}"
        f"&nonce={_base64_url_escape(nonce)}"
        f"&csrf_token={_base64_url_escape(csrf_token)}"
        f"&enckey={_base64_url_escape(base64.b64encode(dec_key).decode())}"
        f"&enciv={_base64_url_escape(base64.b64encode(dec_iv).decode())}"
    )


def _encrypt_login_envelope(pubkey_b64: str, plain_post: str) -> tuple[str, str]:
    aes_key = os.urandom(16)
    iv = os.urandom(16)
    dec_key = os.urandom(16)
    dec_iv = os.urandom(16)

    ct = _aes_cbc_encrypt_sjcl(aes_key, iv, plain_post.encode("utf-8"))

    rsa_der = base64.b64decode(pubkey_b64)
    rsa_key = RSA.import_key(rsa_der)
    rsa_cipher = PKCS1_v1_5.new(rsa_key)
    ck_raw = rsa_cipher.encrypt(f"{base64.b64encode(aes_key).decode()} {base64.b64encode(iv).decode()}".encode("utf-8"))
    ck = _base64_url_escape(base64.b64encode(ck_raw).decode())

    return ct, ck, dec_key, dec_iv


def _login_and_set_cookie(session: requests.Session, host: str, username: str, password: str) -> tuple[bool, str]:
    base = host.rstrip("/")
    page = session.get(f"{base}/?t={int(datetime.now().timestamp() * 1000)}&lang=eng", timeout=20)
    if page.status_code != 200:
        return False, f"login_page_http_{page.status_code}"

    html = page.text
    pubkey = _extract_pubkey_base64(html)
    nonce_match = re.search(r'var\s+nonce\s*=\s*"([^"]+)"', html)
    token_match = re.search(r'var\s+token\s*=\s*"([^"]+)"', html)
    if not pubkey or not nonce_match or not token_match:
        return False, "login_token_extract_failed"

    nonce = nonce_match.group(1)
    csrf_token = token_match.group(1)

    cookie_header = "; ".join([f"{k}={v}" for k, v in session.cookies.get_dict().items()])
    modes = ["plain", "digest"]
    last_reason = "unknown"
    for mode in modes:
        dec_key = os.urandom(16)
        dec_iv = os.urandom(16)
        if mode == "plain":
            plain_post = _build_login_plain_payload(
                username=username,
                password=password,
                nonce=nonce,
                csrf_token=csrf_token,
                dec_key=dec_key,
                dec_iv=dec_iv,
            )
        else:
            plain_post = _build_login_digest_payload(
                username=username,
                password=password,
                nonce=nonce,
                csrf_token=csrf_token,
                dec_key=dec_key,
                dec_iv=dec_iv,
            )

        aes_key = os.urandom(16)
        iv = os.urandom(16)
        ct = _aes_cbc_encrypt_sjcl(aes_key, iv, plain_post.encode("utf-8"))
        rsa_key = RSA.import_key(base64.b64decode(pubkey))
        ck_raw = PKCS1_v1_5.new(rsa_key).encrypt(
            f"{base64.b64encode(aes_key).decode()} {base64.b64encode(iv).decode()}".encode("utf-8")
        )
        ck = _base64_url_escape(base64.b64encode(ck_raw).decode())
        login_post = f"encrypted=1&ct={ct}&ck={ck}"

        headers = {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": f"{base}/",
            "Origin": base,
            "Accept": "*/*",
            "Connection": "close",
        }
        if cookie_header:
            headers["Cookie"] = cookie_header

        response = session.post(f"{base}/login.cgi", data=login_post, headers=headers, timeout=30)
        sid = (response.headers.get("X-SID", "") or "").strip()
        if not sid:
            sid = session.cookies.get("sid", "")
        lsid = session.cookies.get("lsid", "")
        if response.status_code in (200, 299) and sid:
            cookie_parts = [f"sid={sid}"]
            if lsid:
                cookie_parts.append(f"lsid={lsid}")
            if "lang" in session.cookies:
                cookie_parts.append(f"lang={session.cookies.get('lang')}")
            final_cookie = "; ".join(cookie_parts)
            session.headers["Cookie"] = final_cookie
            return True, "ok"

        err_match = re.search(r"err_t\s*=\s*\[([^\]]*)\]", response.text)
        last_reason = f"mode_{mode}_http_{response.status_code}_err_{err_match.group(1).strip() if err_match else 'none'}"

    return False, last_reason


def _encrypt_post_data(pubkey_b64: str, plain_form_data: str) -> str:
    aes_key = os.urandom(16)
    iv = os.urandom(16)
    ct = _aes_cbc_encrypt_sjcl(aes_key, iv, plain_form_data.encode("utf-8"))
    rsa_key = RSA.import_key(base64.b64decode(pubkey_b64))
    ck_raw = PKCS1_v1_5.new(rsa_key).encrypt(
        f"{base64.b64encode(aes_key).decode()} {base64.b64encode(iv).decode()}".encode("utf-8")
    )
    ck = _base64_url_escape(base64.b64encode(ck_raw).decode())
    return f"encrypted=1&ct={ct}&ck={ck}"


def _online_toggle(
    session: requests.Session,
    host: str,
    band: str,
    enabled: bool,
    checks: int,
    interval_s: float,
    wl_id_override: str | None = None,
) -> ToggleEvidence:
    page_url = host.rstrip("/") + _band_path(band)
    try:
        page = session.get(page_url, timeout=10)
    except Exception as error:
        return ToggleEvidence(
            band=band,
            requested_enabled=enabled,
            request_url=page_url,
            request_fields_count=0,
            response_status=0,
            response_len=0,
            response_preview=None,
            confirmed_enabled=None,
            checks=0,
            error_code="session_invalid",
            error_detail=str(error),
        )

    if page.status_code >= 400:
        return ToggleEvidence(
            band=band,
            requested_enabled=enabled,
            request_url=page_url,
            request_fields_count=0,
            response_status=page.status_code,
            response_len=len(page.text),
            response_preview=page.text[:200],
            confirmed_enabled=None,
            checks=0,
            error_code="form_not_found",
            error_detail=f"GET failed: HTTP {page.status_code}",
        )

    summary = summarize_html(band, page.text)
    if not summary.action:
        return ToggleEvidence(
            band=band,
            requested_enabled=enabled,
            request_url=page_url,
            request_fields_count=0,
            response_status=page.status_code,
            response_len=len(page.text),
            response_preview=page.text[:200],
            confirmed_enabled=None,
            checks=0,
            error_code="form_not_found",
            error_detail="No form action found in page",
        )

    payload = _build_toggle_payload(summary, enabled)
    if wl_id_override:
        payload["wl_id"] = wl_id_override
    action_url = urljoin(page_url, summary.action)
    pubkey = _extract_pubkey_base64(page.text)
    if not pubkey:
        return ToggleEvidence(
            band=band,
            requested_enabled=enabled,
            request_url=action_url,
            request_fields_count=len(payload),
            response_status=0,
            response_len=0,
            response_preview=None,
            confirmed_enabled=None,
            checks=0,
            error_code="post_failed",
            error_detail="pubkey_not_found",
        )

    form_data = urlencode(payload)
    encrypted_form_data = _encrypt_post_data(pubkey, form_data)
    try:
        response = session.post(
            action_url,
            data=encrypted_form_data,
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": page_url,
                "Origin": host.rstrip("/"),
                "Accept": "*/*",
                "Connection": "close",
            },
            timeout=10,
        )
    except Exception as error:
        return ToggleEvidence(
            band=band,
            requested_enabled=enabled,
            request_url=action_url,
            request_fields_count=len(payload),
            response_status=0,
            response_len=0,
            response_preview=None,
            confirmed_enabled=None,
            checks=0,
            error_code="post_failed",
            error_detail=str(error),
        )

    confirmed: bool | None = None
    checks_done = 0
    for _ in range(checks):
        checks_done += 1
        confirmed = _confirm_enabled(session, host, band)
        if confirmed is not None and confirmed == enabled:
            break
        if interval_s > 0:
            import time

            time.sleep(interval_s)

    error_code = None
    error_detail = None
    if confirmed is None or confirmed != enabled:
        error_code = "confirmation_timeout"
        error_detail = "State not confirmed in polling window"

    return ToggleEvidence(
        band=band,
        requested_enabled=enabled,
        request_url=action_url,
        request_fields_count=len(payload),
        response_status=response.status_code,
        response_len=len(response.text),
        response_preview=response.text[:200] if response.text else None,
        confirmed_enabled=confirmed,
        checks=checks_done,
        error_code=error_code,
        error_detail=error_detail,
    )


def run_online(args: argparse.Namespace) -> int:
    host = args.host.rstrip("/")
    session = _prepare_session(args.cookie)
    auth_mode = "cookie"
    if not args.cookie:
        if not args.username or not args.password:
            raise SystemExit("Online mode requires --cookie or (--username and --password).")
        auth_mode = "user_password"
        ok, reason = _login_and_set_cookie(session, host, args.username, args.password)
        if not ok:
            payload = {
                "generated_at_utc": _now_iso(),
                "mode": "online_validation",
                "host": host,
                "scenario": args.scenario,
                "overall_status": "failed",
                "auth_mode": auth_mode,
                "auth_error": reason,
                "evidence": [],
            }
            out_path = Path(args.out)
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
            print(f"[FAIL] online evidence (auth failed): {out_path}")
            return 1

    scenarios: list[tuple[str, bool]]
    if args.scenario == "all":
        scenarios = [
            ("24g", False),
            ("24g", True),
            ("5g", False),
            ("5g", True),
        ]
    elif args.scenario == "global_off":
        scenarios = [("24g", False), ("5g", False)]
    elif args.scenario == "global_on":
        scenarios = [("24g", True), ("5g", True)]
    else:
        scenarios = [(args.band, args.enable)]

    evidence: list[dict[str, Any]] = []
    for band, enabled in scenarios:
        result = _online_toggle(
            session=session,
            host=host,
            band=band,
            enabled=enabled,
            checks=args.checks,
            interval_s=args.interval,
            wl_id_override=args.wl_id,
        )
        evidence.append(asdict(result))

    confirmed_count = sum(
        1 for item in evidence if item.get("confirmed_enabled") == item.get("requested_enabled")
    )
    if evidence:
        if confirmed_count == len(evidence):
            overall_status = "success"
        elif confirmed_count == 0:
            overall_status = "failed"
        else:
            overall_status = "partial"
    else:
        overall_status = "failed"

    payload = {
        "generated_at_utc": _now_iso(),
        "mode": "online_validation",
        "host": host,
        "scenario": args.scenario,
        "overall_status": overall_status,
        "auth_mode": auth_mode,
        "evidence": evidence,
    }

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"[OK] online evidence: {out_path}")
    return 0


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Reverse engineering do toggle Wi-Fi do modem (offline + online)."
    )
    sub = parser.add_subparsers(dest="mode", required=True)

    offline = sub.add_parser("offline", help="Extrai contrato de toggle dos HTML capturados.")
    offline.add_argument("--input24", default="modem_captures/wlan_config_2.4g.html")
    offline.add_argument("--input5", default="modem_captures/wlan_config_5g.html")
    offline.add_argument(
        "--out",
        default="docs/wifi_toggle_contract_from_captures.json",
    )
    offline.set_defaults(func=run_offline)

    online = sub.add_parser(
        "online",
        help="Validação controlada no modem real (requer sessão autenticada).",
    )
    online.add_argument("--host", default="http://192.168.1.254")
    online.add_argument(
        "--cookie",
        default=None,
        help='Cookie completo, ex: "sid=...; lsid=..."',
    )
    online.add_argument("--username", default=None)
    online.add_argument("--password", default=None)
    online.add_argument(
        "--scenario",
        choices=["single", "all", "global_off", "global_on"],
        default="single",
    )
    online.add_argument("--band", choices=["24g", "5g"], default="24g")
    online.add_argument(
        "--enable",
        action=argparse.BooleanOptionalAction,
        default=False,
        help="Estado alvo no cenário single (--enable/--no-enable).",
    )
    online.add_argument("--checks", type=int, default=6)
    online.add_argument("--interval", type=float, default=2.0)
    online.add_argument(
        "--wl-id",
        default=None,
        help="Força o SSID id (ex.: 1..4 para 2.4g, 5..8 para 5g).",
    )
    online.add_argument("--out", default="docs/wifi_toggle_online_evidence.json")
    online.set_defaults(func=run_online)

    return parser


def main() -> int:
    parser = _build_parser()
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
