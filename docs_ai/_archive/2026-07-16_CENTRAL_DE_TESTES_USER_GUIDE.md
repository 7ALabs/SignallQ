> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: FUNCIONAL.md secao 4.12 (Central de testes - nomenclatura anterior). Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# Central de Testes — Guia do Usuário

**Para:** Usuários finais, testers, product team  
**Versão:** 0.23.0 (versionCode 56)  
**Data:** 2026-07-05

---

## O que é Central de Testes?

Central de Testes é um espaço da aba **"Explorar ferramentas"** dentro do app SignallQ com 3 ferramentas de diagnóstico:

1. **DNS Benchmark** — Qual servidor DNS é mais rápido?
2. **Ping / Latência** — Qual é meu tempo de resposta?
3. **Diagnóstico Inteligente** — Em breve (análise com IA)

Também exibe um **StatusCard** com informações de conexão em tempo real:
- Se está conectado (Wi-Fi/Móvel)
- Localização do servidor de teste (ex: "São Paulo, BR")

---

## Como Acessar Central de Testes

### Método 1: Pelo botão principal
1. Abra **SignallQ**
2. Toque em **"Central de testes"** (botão grande na home)
3. Aparece a tela SpeedTest com grid de ferramentas

### Método 2: Pela aba de Exploração
1. Na tela SpeedTest, procure aba **"Explorar ferramentas"**
2. Grid com 4 cards aparece
3. Toque em qualquer ferramenta

---

## Usando Ping/Latência

### O que é Ping?

**Ping** mede quanto tempo sua conexão leva para enviar uma requisição para um servidor e receber resposta. É como bater o pé no chão e ouvir o eco voltar.

### Passo a Passo

1. **Abra Central de Testes** (veja acima)
2. **Toque card "Ping / Latência"** (com ícone de "rede check")
3. Modal abre com título "Latência"
4. **Clique botão "Iniciar teste"**
   - Botão desaparece
   - Barra de progresso apareçe (0% → 100%)
   - Teste roda silenciosamente (~20 segundos)
5. **Resultado aparece com 3 números:**

```
Latência:  25 ms
Jitter:     3 ms
Perda:      0%
```

6. **Interprete os resultados:**

| Número | O que significa | Bom é… | Ruim é… |
|--------|-----------------|--------|---------|
| **Latência** | Tempo de ida e volta (ms) | < 50 ms | > 150 ms |
| **Jitter** | Quanto a latência varia entre medições | < 10 ms | > 30 ms |
| **Perda** | Quantos pacotes não voltaram | 0% | > 2% |

7. **Feche modal:**
   - Botão "Voltar" / "Fechar"
   - Ou deslize para baixo

### Exemplos Reais

**Caso 1: Jogador competitivo**
```
Latência: 15 ms ✓ Excelente
Jitter:    2 ms ✓ Estável
Perda:     0%  ✓ Perfeito
→ Ótimo para Valorant, CS:GO, Fortnite
```

**Caso 2: Obra residencial (WiFi fraco)**
```
Latência: 120 ms  ~ Pode ter atraso visível
Jitter:    25 ms  ~ Instável
Perda:      3%    ~ Alguns pacotes perdidos
→ Pode impactar videochamadas
```

**Caso 3: Fibra estável**
```
Latência: 35 ms  ✓ Bom
Jitter:    5 ms  ✓ Estável
Perda:     0%    ✓ Sem perdas
→ Ótimo para trabalho remoto
```

### Quando Usar

- Antes de uma partida online (validar estabilidade)
- Após mudar de operadora (comparar melhoria)
- Quando videochamada está atrasada (diagnosticar)
- Ao configurar WiFi novo (testar canal)

---

## Usando DNS Benchmark

### O que é DNS?

**DNS** é como um guia telefônico da internet. Quando digita "google.com", o DNS traduz para o IP real (142.251.41.14). Quanto mais rápido o DNS, mais rápido seu app carrega.

### O que Mudou na v0.8.5?

**Antes:** 5 servidores (Cloudflare, Google, Quad9, OpenDNS, AdGuard)  
**Agora:** +2 servidores brasileiros (Registro.br, CETIC.br)

#### Novo: Registro.br
- Gerido pelo Fapesp (Federal)
- Mantém registros .br
- Latência baixa em São Paulo
- Confiável, sem tracking

#### Novo: CETIC.br
- Centro de Estudos de Fapesp
- Resolver público nacional
- Ótimo para latência local

### Passo a Passo

1. **Abra Central de Testes**
2. **Toque card "DNS Benchmark"** (ícone de "Speed")
3. Modal abre listando todos os servidores (agora 7, era 5)
4. **Clique "Iniciar teste"**
   - Barra de progresso começa
   - Cada servidor é testado sequencialmente
   - Resultado ranking por velocidade
5. **Leia resultado:**

```
1º lugar: Registro.br ............. 12 ms  ← Campeão!
2º lugar: CETIC.br ................ 18 ms
3º lugar: Cloudflare .............. 25 ms
4º lugar: Google DNS .............. 32 ms
...
```

6. **Interprete:**
   - **Topo = mais rápido** — use este servidor
   - Se Registro.br/CETIC.br estão rápidos → sua ISP está bem posicionada na backbone BR
   - Se servidores internacionais estão lentos → considere trocar ISP ou WiFi

7. **Configure seu dispositivo:**
   - Android: Configurações → Rede → DNS Privado
   - WiFi: Configurações do roteador → DHCP → DNS primário

### DNS que Agora Testamos

| Servidor | URL | Localização | Tipo |
|----------|-----|-------------|------|
| Cloudflare | cloudflare-dns.com | Global | Público |
| Google | dns.google | Global | Público |
| Quad9 | dns.quad9.net | Global | Privado |
| OpenDNS | doh.opendns.com | EUA | Público |
| AdGuard | dns.adguard-dns.com | Global | Bloqueador |
| **Registro.br** | dns.registro.br | **Brasil** | **Público** |
| **CETIC.br** | resolver.cetic.br | **Brasil** | **Público** |

---

## Entendendo Diagnóstico Inteligente

### Status Atual

**Badge: "Em breve"** — Ferramenta ainda em desenvolvimento

Card aparece **apagado** (50% transparência) e **não é clicável**.

### O que Virá

Quando ativado, Diagnóstico Inteligente fará:
1. Analisar sua conexão automaticamente
2. Identificar problemas (fraco sinal, DNS lento, etc.)
3. Gerar recomendações com IA
4. Sugerir ações específicas

Exemplo de recomendação futura:
> "Seu WiFi está no canal 11 (congestionado). Mude para canal 36 (5GHz) para ganhar 30% de velocidade."

### Quando Estará Pronto?

Será lançado quando a flag `FEATURE_DIAGNOSTICO_CHAT` estiver ativa (próximas versões).

---

## StatusCard — Conexão & Servidor

Card no topo exibe:

```
┌─────────────────────────────────┐
│ 📶 Sua Rede: Conectado          │
├─────────────────────────────────┤
│ 🌐 Servidor: São Paulo, BR      │
└─────────────────────────────────┘
```

### Conexão
- **Wi-Fi + ícone** = Conectado via Wi-Fi (mostra SSID)
- **📳 + operadora** = Conectado via 4G/5G
- **❌ Sem conexão** = Desconectado

### Servidor
- Mostra localização do servidor de teste Cloudflare
- Enquanto carrega: "Cloudflare · Carregando…"
- Após carregar: "Cloudflare · São Paulo, BR"

Localização Cloudflare é detectada automaticamente (não configurable pelo usuário).

---

## Troubleshooting

### "Teste não começa"
- Verificar se tem internet (status deve ser "Conectado")
- Tentar novamente (às vezes DNS demora)
- Verificar se SignallQ tem permissão internet (Settings > Permissions)

### "Perda > 0% sempre"
- Sinal WiFi fraco (mudar para mais perto do roteador)
- Interferência (mudar de canal do WiFi)
- Rede móvel instável (trocar de local)

### "Latência muito alta (> 200ms)"
- WiFi longe do roteador
- Rede móvel com cobertura fraca
- Roteador sobrecarregado (desligar aparelhos)

### "StatusCard diz 'Carregando' por muito tempo"
- Normal por ~5 segundos
- Se ficar > 30s, recarregue (pull down) ou reinicie app

---

## Perguntas Frequentes (FAQ)

**P: Diferença entre Latência e Velocidade?**  
R: Latência = tempo de resposta (ms). Velocidade = quanto você baixa/sobe (Mbps). Ambos importam: latência para jogos, velocidade para vídeo.

**P: Posso compartilhar resultado do teste?**  
R: Não (ainda). Resultado fica apenas no seu dispositivo.

**P: Quanto de dados usa?**  
R: Ping = ~100 bytes × 20 = 2KB. DNS = ~200 bytes × 7 = 1.4KB. Quase nada.

**P: Preciso estar conectado em Wi-Fi?**  
R: Não, funciona em 4G/5G também. O teste testa a rede que você está usando.

**P: Qual DNS devo usar?**  
R: Comece com o que aparecer 1º no ranking. Se for Registro.br/CETIC.br = ótimo.

**P: Posso rodar teste múltiplas vezes?**  
R: Sim, sem limite. Cada teste é independente.

---

## Termos Técnicos Simplificados

| Termo | Significado Simples |
|-------|------------------|
| **RTT** (Round Trip Time) | Tempo de ir e voltar |
| **Latência** | Atraso entre enviar e receber |
| **Jitter** | Oscilação da latência |
| **Perda de pacotes** | Mensagens que nunca chegaram |
| **DNS** | Tradutor de endereços internet |
| **HTTP/2** | Forma rápida de comunicação com servidor |
| **DoH** (DNS over HTTPS) | DNS criptografado (mais seguro) |

---

## Feedback & Reporte de Bugs

Se achar que algum teste está errado:
1. Anotar valores (latência, perda, etc.)
2. Tentar novamente em local diferente
3. Reportar em [canal de feedback do SignallQ]

