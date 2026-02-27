# Thunderstruck POC Backend + Painel de Testes

## Melhorias implementadas nesta rodada

- Camunda 7 Embedded segue como engine padrão (`thunderstruck.orchestration.engine=camunda7`).
- Foi adicionado **histórico detalhado do processo** no backend (`PROCESS_HISTORY_EVENT`), registrando cada etapa e transição de status.
- O frontend agora possui um **painel de testes** para simular:
  - fluxo de sucesso,
  - fluxo de falha com rollback SAGA,
  - acompanhamento em tempo real do status,
  - timeline completa do dado (origem -> transformação -> final/rollback).

## Endpoints úteis

- `POST /api/v1/process/simulate` (simula sucesso/falha)
- `GET /api/v1/process/{externalId}/history` (histórico completo)
- `GET /api/v1/process/{externalId}/status` (status atual consolidado)
- `GET /api/v1/process/tickets` (resultado TMF621)

## Tabela de histórico

Tabela Oracle criada em `init.sql`:
- `PROCESS_HISTORY_EVENT`

Campos principais:
- `external_id`, `stage`, `source`, `details`, `previous_status`, `current_status`, `created_at`

## Frontend: painel de simulação

No `thunderstruck-ui`:
- botão **Simular Sucesso**
- botão **Simular Falha com Rollback**
- painel de **status atual**
- painel de **histórico detalhado** por `externalId`
- grade de tickets TMF621 clicável para carregar timeline de um caso existente

## Teste end-to-end

Veja o roteiro detalhado em:
- `docs/E2E_CAMUNDA7.md`
