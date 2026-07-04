import { afterEach, beforeAll, describe, expect, it } from 'vitest';

let readRoute: typeof import('../src/App').readRoute;
let shouldRedirectRecurringUserToHome: typeof import('../src/App').shouldRedirectRecurringUserToHome;

function setHash(hash: string): void {
  (globalThis.window as unknown as { location: { hash: string } }).location.hash = hash;
}

beforeAll(async () => {
  // App.tsx lê `window.location.hash` diretamente; este arquivo roda em ambiente `node`
  // (sem jsdom instalado no projeto), então provemos um `window` mínimo antes de importar.
  Object.defineProperty(globalThis, 'window', {
    configurable: true,
    value: { location: { hash: '' } },
  });
  ({ readRoute, shouldRedirectRecurringUserToHome } = await import('../src/App'));
});

describe('readRoute', () => {
  afterEach(() => {
    setHash('');
  });

  it('resolves landing when there is no hash', () => {
    setHash('');
    expect(readRoute()).toEqual({ kind: 'landing' });
  });

  it('resolves home from #/home', () => {
    setHash('#/home');
    expect(readRoute()).toEqual({ kind: 'home' });
  });

  it('resolves speedtest from #/teste', () => {
    setHash('#/teste');
    expect(readRoute()).toEqual({ kind: 'speedtest' });
  });

  it('resolves result from #/resultado', () => {
    setHash('#/resultado');
    expect(readRoute()).toEqual({ kind: 'result' });
  });

  it('resolves history from #/historico', () => {
    setHash('#/historico');
    expect(readRoute()).toEqual({ kind: 'history' });
  });

  it('resolves testDetail with decoded entry id from #/teste/:id', () => {
    setHash('#/teste/hist%20abc');
    expect(readRoute()).toEqual({ entryId: 'hist abc', kind: 'testDetail' });
  });

  it('resolves report with decoded report id from #/laudo/:id', () => {
    setHash('#/laudo/rep%20123');
    expect(readRoute()).toEqual({ kind: 'report', reportId: 'rep 123' });
  });

  it('falls back to landing for an unknown hash', () => {
    setHash('#/rota-inexistente');
    expect(readRoute()).toEqual({ kind: 'landing' });
  });
});

describe('shouldRedirectRecurringUserToHome', () => {
  it('redirects a recurring user (saved history, no explicit hash) to home', () => {
    expect(
      shouldRedirectRecurringUserToHome({ hash: '', historyEntryCount: 2, historyStatus: 'ready' }),
    ).toBe(true);
  });

  it('does not redirect when there is no saved history', () => {
    expect(
      shouldRedirectRecurringUserToHome({ hash: '', historyEntryCount: 0, historyStatus: 'empty' }),
    ).toBe(false);
  });

  it('does not redirect while history is still loading', () => {
    expect(
      shouldRedirectRecurringUserToHome({ hash: '', historyEntryCount: 0, historyStatus: 'loading' }),
    ).toBe(false);
  });

  it('does not override an explicit hash even with saved history', () => {
    expect(
      shouldRedirectRecurringUserToHome({ hash: '#/ajustes', historyEntryCount: 3, historyStatus: 'ready' }),
    ).toBe(false);
  });
});
