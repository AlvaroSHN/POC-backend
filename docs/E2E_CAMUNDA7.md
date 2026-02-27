# Teste End-to-End detalhado — Camunda 7 + Painel de Simulação

## Objetivo
Validar o fluxo completo e visual:
1. origem do dado,
2. evolução de status no motor,
3. compensação (rollback) quando necessário,
4. resultado final no TMF621,
5. histórico completo visível no frontend.

---

## 1) Infra e banco
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

## 2) Conector CDC
```powershell
./register-connector.ps1
curl.exe http://localhost:8083/connectors/thunderstruck-oracle-connector/status
```
Critério: connector `RUNNING`.

## 3) Subir backend e frontend
```bash
cd ../thunderstruck-bff
mvn spring-boot:run
```

```bash
cd ../thunderstruck-ui
npm start
```

---

## 4) Teste de sucesso (painel)
1. Abrir o painel web.
2. Preencher externalId/descrição/perfil/origem.
3. Clicar **Simular Sucesso**.
4. Verificar no painel:
   - status mudando para etapas intermediárias;
   - timeline com eventos (`API_RECEIVED`, `ORCHESTRATION_STARTED`, `TMF623_CREATED`, `TMF683_CREATED`, `TMF621_PERSISTED`, `RESOLVED`).
5. Verificar tabela canônica:
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT ID, EXTERNAL_ID, STATUS, SEVERITY FROM C##DBZUSER.TROUBLE_TICKET ORDER BY ID DESC FETCH FIRST 10 ROWS ONLY;"
```

## 5) Teste de falha com rollback (painel)
1. Clicar **Simular Falha com Rollback**.
2. Verificar timeline mostrando:
   - decisão de rollback,
   - compensações (`TMF683_COMPENSATED`, `TMF623_COMPENSATED` e, se aplicável, `TMF621_COMPENSATED`),
   - status final `ROLLED_BACK`.
3. Validar ausência de lixo transacional:
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT COUNT(1) AS TOTAL FROM C##DBZUSER.TROUBLE_TICKET WHERE EXTERNAL_ID='<EXTERNAL_ID_FALHA>';"
```
Esperado: `TOTAL = 0`.

## 6) Auditoria de histórico
```sql
docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1 "SELECT EXTERNAL_ID, STAGE, PREVIOUS_STATUS, CURRENT_STATUS, SOURCE, CREATED_AT FROM C##DBZUSER.PROCESS_HISTORY_EVENT ORDER BY ID DESC FETCH FIRST 30 ROWS ONLY;"
```

---

## 7) Novos testes recomendados
- Teste automatizado de controller para `/simulate` com `forceSagaFailure=true/false`.
- Teste de contrato da timeline garantindo ordem cronológica por `createdAt`.
- Teste de resiliência: repetir o mesmo `externalId` e validar idempotência de compensação.
- Teste de carga leve no painel (polling de status/histórico com múltiplos casos).
