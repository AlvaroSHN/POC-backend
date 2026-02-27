# E2E Full Capability — Camunda 7 + DMN + SAGA + User Task

## 0) O que validar
- DMN SLA sem gateways duplicando regra de cliente.
- DMN de roteamento quando ação = escalonar.
- SAGA rollback no erro de criação de caso.
- Histórico detalhado before/after por etapa.
- User task com decisão humana (`finalizar` / `escalar`).
- Persistência completa do contexto TMF:
  - `PROCESS_REQUEST`
  - `CUSTOMER_INTERACTION`
  - `CUSTOMER_INTERACTION_TOPIC`
  - `TROUBLE_TICKET`
  - `PROCESS_HISTORY_EVENT`

## 1) Subida da infraestrutura
```bash
cd thunderstruck-infra
docker-compose down -v
docker-compose up -d
docker ps --format "table {{.Names}}\t{{.Status}}"
```

```bash
docker cp ./oracle/init.sql oracle-thunderstruck:/opt/oracle/init.sql
docker exec -it oracle-thunderstruck sqlplus "sys/password as sysdba" "@/opt/oracle/init.sql"
```

## 2) Registrar CDC
```powershell
./register-connector.ps1
curl.exe http://localhost:8083/connectors/thunderstruck-oracle-connector/status
```

## 3) Subir BFF + UI
```bash
cd ../thunderstruck-bff
mvn clean spring-boot:run
```

```bash
cd ../thunderstruck-ui
npm start
```

## 4) Abrir interfaces
- UI: `http://localhost:3000`
- Camunda Tasklist/Cockpit (dependendo setup): `http://localhost:8080/camunda`
- Operate (apenas cenário Zeebe): `http://localhost:8081`

## 5) Cenários de teste
### 5.1 Sucesso
- `simulateFailAt=NONE`
- após user task, escolher `finalizar`
- esperado: `NOTIFICATION_SENT` e status final `RESOLVED`.

### 5.2 Erro em protocolo
- `simulateFailAt=PROTOCOL`
- esperado: fim em `Falha protocolo`, sem interaction/case.

### 5.3 Erro em interaction
- `simulateFailAt=INTERACTION`
- esperado: protocolo criado e mantido, fim em `Falha interaction`.

### 5.4 Erro em interaction item
- `simulateFailAt=INTERACTION_ITEM`
- esperado: protocolo + interaction mantidos, fim em `Falha interaction item`.

### 5.5 Erro em criação de caso (rollback)
- `simulateFailAt=CASE`
- esperado:
  - rollback do caso,
  - interaction item atualizado para `FAILED_RETRY_ALLOWED`,
  - protocolo + interaction permanecem.

### 5.6 Erro em notificação
- `simulateFailAt=NOTIFICATION`
- esperado: caso criado, fim em `Falha notificação`.

### 5.7 Escalonamento com DMN de roteamento
- `simulateFailAt=NONE`
- na user task escolha `escalar`
- esperado: task DMN `routing-matrix` define `targetQueue` (ex.: `VIP-RETENTION`).

## 6) Consultas SQL de auditoria (completas)

### 6.1 PROCESS_REQUEST (entrada canônica)
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, EXTERNAL_ID, STATUS, DESCRIPTION, CLIENT_TYPE, ORIGIN, CREATED_AT FROM C##DBZUSER.PROCESS_REQUEST ORDER BY ID DESC FETCH FIRST 30 ROWS ONLY;"
```

### 6.2 CUSTOMER_INTERACTION (TMF683 Interaction)
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, INTERACTION_ID, EXTERNAL_ID, CHANNEL, REASON, CUSTOMER_ID, CREATION_DATE FROM C##DBZUSER.CUSTOMER_INTERACTION ORDER BY ID DESC FETCH FIRST 30 ROWS ONLY;"
```

### 6.3 CUSTOMER_INTERACTION_TOPIC (TMF InteractionItem / Topic)
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, TOPIC_ID, INTERACTION_ID, TOPIC_TYPE, STATUS, CREATED_AT FROM C##DBZUSER.CUSTOMER_INTERACTION_TOPIC ORDER BY ID DESC FETCH FIRST 50 ROWS ONLY;"
```

### 6.4 TROUBLE_TICKET (TMF621)
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, EXTERNAL_ID, INTERACTION_ITEM_ID, STATUS, SEVERITY, RELATED_PARTY_ID, CREATED_AT FROM C##DBZUSER.TROUBLE_TICKET ORDER BY ID DESC FETCH FIRST 30 ROWS ONLY;"
```

### 6.5 PROCESS_HISTORY_EVENT (timeline)
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT EXTERNAL_ID, STAGE, PREVIOUS_STATUS, CURRENT_STATUS, DETAILS, CREATED_AT FROM C##DBZUSER.PROCESS_HISTORY_EVENT ORDER BY ID DESC FETCH FIRST 80 ROWS ONLY;"
```

### 6.6 Join de rastreabilidade ponta-a-ponta
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT PR.EXTERNAL_ID, CI.INTERACTION_ID, CIT.TOPIC_ID, TT.ID AS TICKET_ID, TT.STATUS AS TICKET_STATUS FROM C##DBZUSER.PROCESS_REQUEST PR LEFT JOIN C##DBZUSER.CUSTOMER_INTERACTION CI ON CI.EXTERNAL_ID = PR.EXTERNAL_ID LEFT JOIN C##DBZUSER.CUSTOMER_INTERACTION_TOPIC CIT ON CIT.INTERACTION_ID = CI.INTERACTION_ID LEFT JOIN C##DBZUSER.TROUBLE_TICKET TT ON TT.INTERACTION_ITEM_ID = CIT.TOPIC_ID WHERE PR.EXTERNAL_ID = 'CASE-EXEMPLO' ORDER BY PR.ID DESC;"
```

## 7) Como enxergar before/after na UI
A timeline no React exibe por evento o `details` no padrão:
- `before={x,y,z} | after={xx,y,a}`

Com isso você compara entrada e saída de cada “caixinha” BPMN.

## 8) Diagrama completo da POC (Mermaid)
```mermaid
flowchart TD
    A[UI React<br/>/api/v1/process/simulate] --> B[BFF ProcessController]
    A2[CDC Oracle Debezium Kafka] --> B2[CdcEventConsumer]

    B --> C[ProcessService]
    B2 --> C

    C --> D{thunderstruck.orchestration.engine}
    D -->|camunda7| E[Camunda7OrchestrationEngine]
    D -->|zeebe| Z[ZeebeOrchestrationEngine]

    E --> P[Processo BPMN<br/>thunderstruck-saga-process]

    %% happy path
    P --> S1[GenerateProtocolDelegate]
    S1 --> S2[CreateCustomerInteractionDelegate]
    S2 --> S3[CreateCustomerInteractionItemDelegate]
    S3 --> S4[DMN SLA<br/>sla-by-client]
    S4 --> S5[CreateTroubleTicketCaseDelegate]
    S5 --> U[UserTask: Ação do analista]

    U --> G{acao}
    G -->|finalizar| N[NotifyCustomerDelegate]
    G -->|escalar| R1[DMN Routing<br/>routing-matrix]
    R1 --> R2[TransferQueue]
    R2 --> N

    N --> T[TMF621 Persist<br/>Tmf621PersistTroubleTicketDelegate]
    T --> OK[End: RESOLVED]

    %% simulated errors and saga branches
    S1 -. simulateFailAt=PROTOCOL .-> EP[End: Falha protocolo]
    S2 -. simulateFailAt=INTERACTION .-> EI[End: Falha interaction]
    S3 -. simulateFailAt=INTERACTION_ITEM .-> EII[End: Falha interaction item]
    N  -. simulateFailAt=NOTIFICATION .-> EN[End: Falha notificação]

    S5 -. simulateFailAt=CASE .-> RB1[RollbackCaseDelegate]
    RB1 --> RB2[UpdateInteractionItemFailureDelegate]
    RB2 --> ERB[End: Rolled back]

    %% persistence layer
    S2 --> DBI[(CUSTOMER_INTERACTION)]
    S3 --> DBT[(CUSTOMER_INTERACTION_TOPIC)]
    T --> DBTT[(TROUBLE_TICKET)]
    C --> DBPR[(PROCESS_REQUEST)]

    S1 --> H[(PROCESS_HISTORY_EVENT)]
    S2 --> H
    S3 --> H
    S5 --> H
    N --> H
    T --> H
    RB2 --> H

    %% read APIs
    B --> Q1[GET /api/v1/process/{externalId}/history]
    B --> Q2[GET /api/v1/process/{externalId}/status]
    B --> Q3[GET /api/v1/process/{externalId}/interactions]
    B --> Q4[GET /api/v1/process/interactions/{interactionId}/topics]
    B --> Q5[GET /api/v1/process/tickets]

    Q1 --> H
    Q2 --> H
    Q3 --> DBI
    Q4 --> DBT
    Q5 --> DBTT
```
