# Limpa conector anterior (se houver lixo)
curl.exe -X DELETE http://localhost:8083/connectors/thunderstruck-oracle-connector

# Espera 2 segundos para o Debezium respirar
Start-Sleep -Seconds 2

# Registra o novo conector
curl.exe -X POST -H "Content-Type: application/json" --data "@.\debezium\oracle-connector.json" http://localhost:8083/connectors

# Mostra o status
Start-Sleep -Seconds 2
curl.exe http://localhost:8083/connectors/thunderstruck-oracle-connector/status