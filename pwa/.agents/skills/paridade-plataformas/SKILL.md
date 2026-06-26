---
name: paridade-plataformas
description: Use quando uma feature, regra de diagnóstico, payload, copy ou comportamento precisar ser comparado entre SignallQ PWA e Android. Classifica como equivalente, degradado, impossível ou pendente.
---

# Paridade de Plataformas — SignallQ

## Objetivo

Garantir paridade honesta entre PWA e Android sem fingir que navegador tem capacidades nativas.

## Classificações

### Equivalente

A feature pode existir no PWA com comportamento próximo ao Android.

### Degradado

A feature pode existir no PWA, mas com menor precisão, menor cobertura ou mais limitação.

### Impossível

A feature depende de API nativa Android sem equivalente web confiável.

### Pendente

Ainda falta investigar ou fechar contrato técnico.

## Exemplos de diferenças

Android pode ter acesso nativo a recursos que o PWA não tem, como:

- métricas avançadas de Wi-Fi;
- RSSI e sinal real;
- informações de rede local;
- APIs nativas de conectividade;
- serviços em foreground;
- integração mais profunda com sistema.

PWA deve focar em:

- speedtest web;
- diagnóstico baseado em HTTP/timing;
- histórico local;
- instalação web;
- experiência leve via link;
- diagnóstico simples e acionável.

## Regra de ouro

Paridade não é copiar tela.

Paridade é entregar a mesma promessa central dentro das limitações reais da plataforma.

## Antes de decidir

Verifique:

- Qual é a promessa da feature?
- O PWA consegue medir isso de verdade?
- A precisão é comparável?
- Existe fallback honesto?
- O usuário precisa saber da limitação?
- A mudança afeta contrato compartilhado com Android?

## Saída esperada

Ao usar esta skill, entregue uma tabela com:

- feature;
- Android;
- PWA;
- classificação;
- risco;
- recomendação.

## Bloqueio

Se a mudança afetar contrato compartilhado com Android, pare e peça revisão cruzada antes de implementar.
