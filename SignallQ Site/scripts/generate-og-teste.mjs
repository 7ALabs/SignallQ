// Gera a imagem Open Graph (1200x630) da rota /teste, localmente, sem serviço
// externo. Composição: fundo gradiente (mesma paleta do card de anúncio do
// SiteFooter), lockup oficial do SignallQ (marca do produto, não a antiga
// marca institucional `7alabs-*`), título/subtítulo do convite e duas
// screenshots reais do app (mesmas usadas na galeria da página) — Início
// (modo escuro) e Modo Gamer, os dois escolhidos pra já comunicar um
// diferencial do produto na prévia de compartilhamento (curadoria 2026-07-29).
//
// As screenshots de `public/teste/` são cruas (sem bezel desenhado na
// imagem, ao contrário do release anterior que usava os PNGs já emoldurados
// de `android/assets/store/screenshots/framed/`) — este script desenha um
// bezel simples via máscara SVG antes de rotacionar, pra manter a leitura de
// "smartphone" na miniatura do OG.
//
// Rodar manualmente após trocar a copy do título/subtítulo desta rota, ou
// quando os assets de marca/screenshots mudarem — não faz parte do `npm run
// build` (é conteúdo estático versionado, não build derivado do código-fonte).
//
// Uso: node scripts/generate-og-teste.mjs
import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import sharp from 'sharp'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..')
const SCREENSHOTS_DIR = path.join(ROOT, 'public', 'teste')
const OUTPUT_DIR = path.join(ROOT, 'public', 'og')
const OUTPUT_FILE = path.join(OUTPUT_DIR, 'teste.png')
const BEZEL = 14
const BEZEL_RADIUS = 48
const BEZEL_COLOR = '#16181d'

// Envolve uma screenshot crua num bezel escuro simples (cantos arredondados),
// pra simular um smartphone antes de rotacionar — equivalente em sharp da
// moldura CSS usada na galeria da própria página (`PhoneShot` em `TestePage.tsx`).
async function framedShot(filePath, targetHeight) {
  const raw = await sharp(filePath).resize({ height: targetHeight - BEZEL * 2 }).png().toBuffer()
  const meta = await sharp(raw).metadata()
  const width = (meta.width ?? 0) + BEZEL * 2
  const height = (meta.height ?? 0) + BEZEL * 2

  const mask = Buffer.from(
    `<svg width="${width}" height="${height}"><rect width="${width}" height="${height}" rx="${BEZEL_RADIUS}" fill="#fff"/></svg>`
  )

  const bezel = Buffer.from(
    `<svg width="${width}" height="${height}"><rect width="${width}" height="${height}" rx="${BEZEL_RADIUS}" fill="${BEZEL_COLOR}"/></svg>`
  )

  const withBezel = await sharp(bezel)
    .composite([{ input: raw, left: BEZEL, top: BEZEL }])
    .png()
    .toBuffer()

  return sharp(withBezel)
    .composite([{ input: mask, blend: 'dest-in' }])
    .png()
    .toBuffer()
}

const WIDTH = 1200
const HEIGHT = 630

async function main() {
  await mkdir(OUTPUT_DIR, { recursive: true })

  const backgroundSvg = Buffer.from(`
    <svg width="${WIDTH}" height="${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#2A1E52" />
          <stop offset="55%" stop-color="#17132B" />
          <stop offset="100%" stop-color="#0E0D14" />
        </linearGradient>
        <radialGradient id="glow" cx="82%" cy="10%" r="55%">
          <stop offset="0%" stop-color="rgba(124,77,255,0.38)" />
          <stop offset="100%" stop-color="rgba(124,77,255,0)" />
        </radialGradient>
      </defs>
      <rect width="${WIDTH}" height="${HEIGHT}" fill="url(#bg)" />
      <rect width="${WIDTH}" height="${HEIGHT}" fill="url(#glow)" />
    </svg>
  `)

  const textSvg = Buffer.from(`
    <svg width="${WIDTH}" height="${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
      <text x="64" y="290" font-family="Arial, sans-serif" font-size="58" font-weight="700" fill="#FFFFFF">Ajude a testar</text>
      <text x="64" y="358" font-family="Arial, sans-serif" font-size="58" font-weight="700" fill="#FFFFFF">o SignallQ</text>
      <text x="64" y="410" font-family="Arial, sans-serif" font-size="27" font-weight="500" fill="rgba(255,255,255,0.72)">Teste fechado para Android</text>
    </svg>
  `)

  const logoBuffer = await sharp(path.join(ROOT, 'public', 'brand', 'signallq-lockup-dark-bg.png'))
    .resize({ width: 300 })
    .png()
    .toBuffer()

  // Screenshots reais do app, já emolduradas por `framedShot` (bezel escuro +
  // cantos arredondados). rotate() preenche os cantos vazados pela rotação
  // com transparência real (precisa de canal alfa antes de rotacionar).
  const screenshotA = await sharp(await framedShot(path.join(SCREENSHOTS_DIR, '01-home-dark.png'), 560))
    .ensureAlpha()
    .rotate(-6, { background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer()

  const screenshotB = await sharp(await framedShot(path.join(SCREENSHOTS_DIR, '06-modo-gamer.png'), 560))
    .ensureAlpha()
    .rotate(6, { background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer()

  const metaA = await sharp(screenshotA).metadata()
  const metaB = await sharp(screenshotB).metadata()

  await sharp(backgroundSvg)
    .composite([
      { input: screenshotB, left: WIDTH - (metaB.width ?? 340) - 40, top: HEIGHT - (metaB.height ?? 620) + 40 },
      { input: screenshotA, left: WIDTH - (metaA.width ?? 340) - (metaB.width ?? 340) + 60, top: HEIGHT - (metaA.height ?? 620) + 20 },
      { input: textSvg, left: 0, top: 0 },
      { input: logoBuffer, left: 64, top: 56 },
    ])
    .png()
    .toFile(OUTPUT_FILE)

  console.log(`Imagem OG gerada em ${path.relative(ROOT, OUTPUT_FILE)}`)
}

main().catch((err) => {
  console.error(err)
  process.exitCode = 1
})
