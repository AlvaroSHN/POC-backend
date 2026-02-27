# dashboard-thunderstruck.ps1

# 1. Abrir URLs no Navegador
Write-Host "Abrindo Operate e Tasklist..." -ForegroundColor Cyan
Start-Process "http://localhost:8081" # Operate
Start-Process "http://localhost:8082" # Tasklist

# 2. Janela para LOGS DO BFF (Spring Boot)
Write-Host "Iniciando logs do BFF..." -ForegroundColor Yellow
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "cd .\thunderstruck-bff\; Write-Host '--- LOGS DO BFF ---' -ForegroundColor Yellow; mvn spring-boot:run"

# 3. Janela para LOGS DO DEBEZIUM (CDC)
Write-Host "Iniciando logs do Debezium..." -ForegroundColor Magenta
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "Write-Host '--- LOGS DO DEBEZIUM ---' -ForegroundColor Magenta; docker logs -f thunderstruck-infra-debezium-1"

# 4. Janela para CONSUMER KAFKA (Ver os eventos chegando)
Write-Host "Iniciando Consumer Kafka..." -ForegroundColor Green
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "Write-Host '--- EVENTOS NO KAFKA ---' -ForegroundColor Green; docker exec -it thunderstruck-infra-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic thunderstruck-cdc.C__DBZUSER.PROCESS_REQUEST --from-beginning"

# 5. Janela para CONSOLE ORACLE (Para fazer os INSERTS)
Write-Host "Iniciando Console Oracle..." -ForegroundColor White
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "Write-Host '--- CONSOLE ORACLE (SQLPLUS) ---' -ForegroundColor White; docker exec -it oracle-thunderstruck sqlplus c##dbzuser/dbzpassword@FREEPDB1"

Write-Host "--- Painel Thunderstruck pronto! ---" -ForegroundColor Green