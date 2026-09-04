# Script de test de l'API Chatbot
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Tests API Chatbot IaC" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[TEST 1] Health Check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8081/api/chatbot/health" -Method Get
    Write-Host "✅ Health: $health" -ForegroundColor Green
} catch {
    Write-Host "❌ Health check failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Test Endpoint
Write-Host "[TEST 2] Test Endpoint..." -ForegroundColor Yellow
try {
    $test = Invoke-RestMethod -Uri "http://localhost:8081/api/chatbot/test" -Method Get
    Write-Host "✅ Test: $test" -ForegroundColor Green
} catch {
    Write-Host "❌ Test endpoint failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Get All Requests
Write-Host "[TEST 3] Get All Requests..." -ForegroundColor Yellow
try {
    $requests = Invoke-RestMethod -Uri "http://localhost:8081/api/chatbot/requests" -Method Get
    Write-Host "✅ Nombre de requêtes: $($requests.Count)" -ForegroundColor Green
    if ($requests.Count -gt 0) {
        Write-Host "   Première requête:" -ForegroundColor Gray
        Write-Host "   - ID: $($requests[0].id)" -ForegroundColor Gray
        Write-Host "   - Status: $($requests[0].status)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Get requests failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Vérifier Ollama
Write-Host "[TEST 4] Vérification Ollama..." -ForegroundColor Yellow
try {
    $ollama = Invoke-RestMethod -Uri "http://localhost:11434/api/version" -Method Get
    Write-Host "✅ Ollama est en cours d'exécution: Version $($ollama.version)" -ForegroundColor Green
    $ollamaRunning = $true
} catch {
    Write-Host "⚠️  Ollama n'est PAS en cours d'exécution!" -ForegroundColor Red
    Write-Host "   Pour démarrer Ollama: ollama serve" -ForegroundColor Yellow
    $ollamaRunning = $false
}
Write-Host ""

# Test 5: Process Message (seulement si Ollama est disponible)
if ($ollamaRunning) {
    Write-Host "[TEST 5] Process Message (avec Ollama)..." -ForegroundColor Yellow
    try {
        $body = @{
            message = "Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM"
            targetPlatform = "terraform"
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "http://localhost:8081/api/chatbot/process" -Method Post -ContentType "application/json" -Body $body
        Write-Host "✅ Message traité avec succès!" -ForegroundColor Green
        Write-Host "   - Request ID: $($response.requestId)" -ForegroundColor Gray
        Write-Host "   - Status: $($response.status)" -ForegroundColor Gray
        Write-Host "   - Resource Type: $($response.extractedParams.resourceType)" -ForegroundColor Gray
        Write-Host "   - Message: $($response.message)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   Code Terraform généré:" -ForegroundColor Gray
        Write-Host "   $($response.generatedCode.Substring(0, [Math]::Min(200, $response.generatedCode.Length)))..." -ForegroundColor DarkGray
    } catch {
        Write-Host "❌ Process message failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "[TEST 5] Process Message - SKIPPED (Ollama non disponible)" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
