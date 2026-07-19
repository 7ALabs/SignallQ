/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SIGNALLQ_BETA_DOWNLOAD_URL?: string
  readonly VITE_ADSENSE_PUBLISHER_ID?: string
  readonly VITE_ADSENSE_SLOT_RESULT?: string
  readonly VITE_SPEEDTEST_DOWNLOAD_URL?: string
  readonly VITE_SPEEDTEST_UPLOAD_URL?: string
  readonly VITE_SPEEDTEST_SERVER_LABEL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
