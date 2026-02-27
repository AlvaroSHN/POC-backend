# E2E Full Capability — Camunda 7 + DMN + SAGA + User Task

## 0) O que validar
- DMN SLA sem gateways duplicando regra de cliente.
- DMN de roteamento quando ação = escalonar.
- SAGA rollback no erro de criação de caso.
- Histórico detalhado before/after por etapa.
- User task com formulário para decisão humana.

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
mvn spring-boot:run
```
```bash
cd ../thunderstruck-ui
npm start
```

## 4) Abrir interfaces
- UI: `http://localhost:3000`
- Camunda Tasklist/Cockpit (dependendo setup): `http://localhost:8080/camunda`

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

## 6) Consultas SQL de auditoria
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, EXTERNAL_ID, STATUS, SEVERITY FROM C##DBZUSER.TROUBLE_TICKET ORDER BY ID DESC FETCH FIRST 20 ROWS ONLY;"
```

```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT EXTERNAL_ID, STAGE, PREVIOUS_STATUS, CURRENT_STATUS, DETAILS, CREATED_AT FROM C##DBZUSER.PROCESS_HISTORY_EVENT ORDER BY ID DESC FETCH FIRST 50 ROWS ONLY;"
```

## 7) Como enxergar before/after na UI
A timeline no React exibe por evento o `details` no padrão:
- `before={x,y,z} | after={xx,y,a}`

Isso permite comparar entrada e saída de cada “caixinha” BPMN.
