// Gera a imagem Open Graph (1200x630) da rota /teste, localmente, sem serviço
// externo. Composição: fundo gradiente (mesma paleta do card de anúncio do
// SiteFooter), lockup oficial do SignallQ (marca do produto, não a antiga
// marca institucional `7alabs-*`), título/subtítulo do convite e duas
// screenshots reais do app (mesmas usadas na galeria da página).
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
const SCREENSHOTS_DIR = path.resolve(ROOT, '../android/assets/store/screenshots/framed')
const OUTPUT_DIR = path.join(ROOT, 'public', 'og')
const OUTPUT_FILE = path.join(OUTPUT_DIR, 'teste.png')

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

  // Screenshots reais do app (fundo violeta já embutido no frame — funde com
  // o gradiente de fundo do card). rotate() preenche os cantos vazados com
  // transparência real (precisa de canal alfa antes de rotacionar).
  const screenshotA = await sharp(path.join(SCREENSHOTS_DIR, '01-home.png'))
    .resize({ height: 560 })
    .ensureAlpha()
    .rotate(-6, { background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer()

  const screenshotB = await sharp(path.join(SCREENSHOTS_DIR, '03-speedtest-resultado.png'))
    .resize({ height: 560 })
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
