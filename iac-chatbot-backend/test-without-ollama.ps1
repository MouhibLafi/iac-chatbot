# Test des endpoints qui NE nécessitent PAS Ollama
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Tests Backend (sans Ollama)" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8081/api/chatbot"
$allPassed = $true

# Fonction de test
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null
    )
    
    Write-Host "[$Name]" -ForegroundColor Yellow -NoNewline
    Write-Host " $Method $Uri" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
        }
        
        if ($Body) {
            $params.Body = $Body | ConvertTo-Json
            $params.ContentType = "application/json"
        }
        
        $result = Invoke-RestMethod @params
        Write-Host "  ✅ SUCCESS" -ForegroundColor Green
        return $result
    } catch {
        Write-Host "  ❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $script:allPassed = $false
        return $null
    }
}

# Test 1: Health
Write-Host ""
Write-Host "Test 1: Health Check" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor DarkGray
$health = Test-Endpoint -Name "Health" -Method "GET" -Uri "$baseUrl/health"
if ($health) {
    Write-Host "  Response: $health" -ForegroundColor DarkGray
}

# Test 2: Test endpoint
Write-Host ""
Write-Host "Test 2: Test Endpoint" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor DarkGray
$test = Test-Endpoint -Name "Test" -Method "GET" -Uri "$baseUrl/test"
if ($test) {
    Write-Host "  Response: $test" -ForegroundColor DarkGray
}

# Test 3: Get all requests (vide au début)
Write-Host ""
Write-Host "Test 3: Get All Requests" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor DarkGray
$requests = Test-Endpoint -Name "GetAll" -Method "GET" -Uri "$baseUrl/requests"
if ($requests -ne $null) {
    Write-Host "  Count: $($requests.Count) requests" -ForegroundColor DarkGray
}

# Test 4: Get by invalid ID (devrait retourner 404)
Write-Host ""
Write-Host "Test 4: Get Non-Existent Request" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor DarkGray
Write-Host "[GetById]" -ForegroundColor Yellow -NoNewline
Write-Host " GET $baseUrl/requests/999" -ForegroundColor Gray
try {
    $notFound = Invoke-RestMethod -Uri "$baseUrl/requests/999" -Method GET
    Write-Host "  ❌ Should return 404" -ForegroundColor Red
    $allPassed = $false
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "  ✅ SUCCESS (404 as expected)" -ForegroundColor Green
    } else {
        Write-Host "  ❌ FAILED: Wrong status code" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 5: Get by status (vide)
Write-Host ""
Write-Host "Test 5: Get Requests by Status" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor DarkGray
$completed = Test-Endpoint -Name "ByStatus" -Method "GET" -Uri "$baseUrl/requests/status/completed"
if ($completed -ne $null) {
    Write-Host "  Count: $($completed.Count) completed requests" -ForegroundColor DarkGray
}

# Résumé
Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "✅ TOUS LES TESTS RÉUSSIS!" -ForegroundColor Green
} else {
    Write-Host "❌ CERTAINS TESTS ONT ÉCHOUÉ" -ForegroundColor Red
}
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Note sur Ollama
Write-Host "Note:" -ForegroundColor Blue
Write-Host "   L'endpoint POST /process necessite Ollama en cours d'execution" -ForegroundColor Gray
Write-Host "   Pour tester cet endpoint:" -ForegroundColor Gray
Write-Host "   1. Executer: ollama serve" -ForegroundColor Yellow
Write-Host "   2. Puis: powershell test-api.ps1" -ForegroundColor Yellow
Write-Host ""
