interface SettingsPanelProps {
  historyCount: number;
  onClearHistory: () => void;
  onGoHome: () => void;
}

export function SettingsPanel({ historyCount, onClearHistory, onGoHome }: SettingsPanelProps) {
  return (
    <main className="settings-panel" aria-label="Ajustes e privacidade">
      <section className="settings-panel__hero">
        <p className="overline">Ajustes</p>
        <h1>Preferências úteis da PWA</h1>
        <p>
          O SignallQ Web mede a experiência da conexão pelo navegador. Histórico e preferências ficam neste dispositivo.
        </p>
      </section>

      <section className="settings-section">
        <div>
          <p className="overline">Histórico local</p>
          <h2>Medições salvas</h2>
          <p>{historyCount} medição(ões) salvas neste navegador. Limpar dados remove também os laudos locais.</p>
        </div>
        <button className="text-button text-button--danger" disabled={historyCount === 0} type="button" onClick={onClearHistory}>
          Limpar histórico
        </button>
      </section>

      <section className="settings-section">
        <div>
          <p className="overline">Limitações web</p>
          <h2>O que o navegador não mede</h2>
          <p>
            A PWA não acessa RSSI, redes Wi-Fi próximas, MAC, canal, torres de celular, ping ICMP real ou scan de dispositivos.
            Quando uma métrica não existir na web, ela aparece como não medida.
          </p>
        </div>
      </section>

      <section className="settings-section">
        <div>
          <p className="overline">Privacidade</p>
          <h2>Sem login no MVP</h2>
          <p>
            O histórico inicial é local. Se a análise IA estiver disponível, somente métricas estruturadas do teste são enviadas
            ao Worker, sem senha ou dado sensível.
          </p>
        </div>
      </section>

      <button className="text-button" type="button" onClick={onGoHome}>
        Voltar ao início
      </button>
    </main>
  );
}
