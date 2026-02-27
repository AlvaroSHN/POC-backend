# Thunderstruck — Pacote Full Capability (Camunda 7 + SAGA + DMN + User Task)

## Aderência à arquitetura narrada
- Entrada assíncrona: API/CDC -> Kafka -> BFF -> Orquestração.
- Orquestração no Camunda 7 Embedded com BPMN robusto e ramificações.
- Decisões por DMN (SLA por tipo de cliente + matriz de roteamento).
- Persistência canônica TMF621 (`TROUBLE_TICKET`) e trilha completa (`PROCESS_HISTORY_EVENT`).
- SAGA com rollback de caso + atualização de interaction item para retry.

## Fluxo principal (caso de reclamação)
1. Gerar protocolo (serviço fake random).
2. Criar Customer Interaction (protocolo permanece).
3. Criar Customer Interaction Item (topic).
4. DMN define SLA por tipo de cliente.
5. Criar caso (TroubleTicket).
6. User Task: analista decide `escalar` ou `finalizar`.
7. Se escalonar: DMN matriz de roteamento define fila.
8. Notificação ao cliente.

## Simulações suportadas
No `simulateFailAt`:
- `NONE`
- `PROTOCOL`
- `INTERACTION`
- `INTERACTION_ITEM`
- `CASE`
- `NOTIFICATION`

## Artefatos-chave
- BPMN: `thunderstruck-bff/src/main/resources/processes/thunderstruck-camunda7-saga.bpmn`
- DMN SLA: `thunderstruck-bff/src/main/resources/processes/sla-by-client.dmn`
- DMN Roteamento: `thunderstruck-bff/src/main/resources/processes/routing-matrix.dmn`
- User Task Form: `thunderstruck-bff/src/main/resources/static/forms/case-decision-form.html`
- E2E detalhado: `docs/E2E_CAMUNDA7.md`


## Nota importante de execução do Camunda 7

Para evitar erro de `PlatformTransactionManager` no startup do Camunda 7 embedded, a aplicação agora sobe um datasource JDBC H2 **somente para o engine Camunda** (`spring.datasource`) enquanto mantém Oracle via R2DBC para os dados da POC.
