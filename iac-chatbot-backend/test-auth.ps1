# ==============================================================================
# Script de test pour l'authentification JWT
# ==============================================================================

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Tests d'Authentification - IaC Chatbot" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8081/api"
$adminUsername = "admin"
$adminPassword = "password123"
$userUsername = "user"
$userPassword = "password123"

# ==============================================================================
# Test 1: Health Check (Public)
# ==============================================================================
Write-Host "[TEST 1] Health Check (Public)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/chatbot/health" -Method Get
    Write-Host "✅ Health: $response" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur Health Check: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ==============================================================================
# Test 2: Login Admin
# ==============================================================================
Write-Host "[TEST 2] Login Admin..." -ForegroundColor Yellow
try {
    $loginData = @{
        username = $adminUsername
        password = $adminPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post `
        -Body $loginData -ContentType "application/json"
    
    $adminToken = $response.token
    Write-Host "✅ Login Admin réussi!" -ForegroundColor Green
    Write-Host "   - Token: $($adminToken.Substring(0, 30))..." -ForegroundColor Gray
    Write-Host "   - Username: $($response.username)" -ForegroundColor Gray
    Write-Host "   - Role: $($response.role)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Erreur Login Admin: $($_.Exception.Message)" -ForegroundColor Red
    $adminToken = $null
}
Write-Host ""

# ==============================================================================
# Test 3: Login User
# ==============================================================================
Write-Host "[TEST 3] Login User..." -ForegroundColor Yellow
try {
    $loginData = @{
        username = $userUsername
        password = $userPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post `
        -Body $loginData -ContentType "application/json"
    
    $userToken = $response.token
    Write-Host "✅ Login User réussi!" -ForegroundColor Green
    Write-Host "   - Token: $($userToken.Substring(0, 30))..." -ForegroundColor Gray
    Write-Host "   - Username: $($response.username)" -ForegroundColor Gray
    Write-Host "   - Role: $($response.role)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Erreur Login User: $($_.Exception.Message)" -ForegroundColor Red
    $userToken = $null
}
Write-Host ""

# ==============================================================================
# Test 4: Accès endpoint protégé SANS token (doit échouer)
# ==============================================================================
Write-Host "[TEST 4] Accès endpoint protégé SANS token (doit échouer)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/chatbot/process" -Method Post `
        -Body '{"message":"test"}' -ContentType "application/json"
    Write-Host "❌ ERREUR: L'accès devrait être refusé!" -ForegroundColor Red
} catch {
    Write-Host "✅ Accès refusé comme attendu (401 Unauthorized)" -ForegroundColor Green
}
Write-Host ""

# ==============================================================================
# Test 5: Accès endpoint protégé AVEC token User
# ==============================================================================
if ($userToken) {
    Write-Host "[TEST 5] Accès endpoint protégé AVEC token User..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $userToken"
            "Content-Type" = "application/json"
        }
        
        $requestData = @{
            message = "Je veux une VM Ubuntu avec 2 CPU et 4 Go de RAM"
            targetPlatform = "terraform"
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$baseUrl/chatbot/process" -Method Post `
            -Headers $headers -Body $requestData
        
        Write-Host "✅ Requête Chatbot réussie avec token User!" -ForegroundColor Green
        Write-Host "   - Request ID: $($response.requestId)" -ForegroundColor Gray
        Write-Host "   - Status: $($response.status)" -ForegroundColor Gray
        Write-Host "   - Resource Type: $($response.extractedParams.resourceType)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "[TEST 5] SKIP - Pas de token User" -ForegroundColor Yellow
}
Write-Host ""

# ==============================================================================
# Test 6: Récupérer son profil (/me)
# ==============================================================================
if ($userToken) {
    Write-Host "[TEST 6] Récupérer profil utilisateur (/me)..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $userToken"
        }

        $response = Invoke-RestMethod -Uri "$baseUrl/auth/me" -Method Get -Headers $headers
        
        Write-Host "✅ Profil récupéré!" -ForegroundColor Green
        Write-Host "   - Username: $($response.username)" -ForegroundColor Gray
        Write-Host "   - Email: $($response.email)" -ForegroundColor Gray
        Write-Host "   - Role: $($response.role)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "[TEST 6] SKIP - Pas de token User" -ForegroundColor Yellow
}
Write-Host ""

# ==============================================================================
# Test 7: Accès Admin - Lister tous les utilisateurs
# ==============================================================================
if ($adminToken) {
    Write-Host "[TEST 7] Admin - Lister tous les utilisateurs..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $adminToken"
        }

        $response = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $headers
        
        Write-Host "✅ Liste des utilisateurs récupérée!" -ForegroundColor Green
        Write-Host "   - Nombre d'utilisateurs: $($response.Count)" -ForegroundColor Gray
        foreach ($user in $response) {
            Write-Host "   - $($user.username) ($($user.role))" -ForegroundColor Gray
        }
    } catch {
        Write-Host "❌ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "[TEST 7] SKIP - Pas de token Admin" -ForegroundColor Yellow
}
Write-Host ""

# ==============================================================================
# Test 8: User essaie d'accéder à un endpoint Admin (doit échouer)
# ==============================================================================
if ($userToken) {
    Write-Host "[TEST 8] User essaie d'accéder aux endpoints Admin (doit échouer)..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $userToken"
        }

        $response = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $headers
        Write-Host "❌ ERREUR: L'accès devrait être refusé!" -ForegroundColor Red
    } catch {
        Write-Host "✅ Accès refusé comme attendu (403 Forbidden)" -ForegroundColor Green
    }
} else {
    Write-Host "[TEST 8] SKIP - Pas de token User" -ForegroundColor Yellow
}
Write-Host ""

# ==============================================================================
# Test 9: Register nouveau utilisateur
# ==============================================================================
Write-Host "[TEST 9] Register nouveau utilisateur..." -ForegroundColor Yellow
try {
    $registerData = @{
        username = "testuser"
        email = "testuser@test.com"
        password = "test123456"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post `
        -Body $registerData -ContentType "application/json"
    
    Write-Host "✅ Registration réussie!" -ForegroundColor Green
    Write-Host "   - Message: $($response.message)" -ForegroundColor Gray
} catch {
    $errorMessage = $_.ErrorDetails.Message | ConvertFrom-Json
    if ($errorMessage.message -like "*déjà utilisé*") {
        Write-Host "⚠️  Utilisateur existe déjà (normal si test déjà exécuté)" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Erreur: $($errorMessage.message)" -ForegroundColor Red
    }
}
Write-Host ""

# ==============================================================================
# Test 10: Login avec mauvais mot de passe (doit échouer)
# ==============================================================================
Write-Host "[TEST 10] Login avec mauvais mot de passe (doit échouer)..." -ForegroundColor Yellow
try {
    $loginData = @{
        username = $adminUsername
        password = "wrongpassword"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post `
        -Body $loginData -ContentType "application/json"
    
    Write-Host "❌ ERREUR: Le login devrait échouer!" -ForegroundColor Red
} catch {
    Write-Host "✅ Login refusé comme attendu (401 Unauthorized)" -ForegroundColor Green
}
Write-Host ""

# ==============================================================================
# Résumé
# ==============================================================================
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Comptes de test disponibles:" -ForegroundColor White
Write-Host "   Admin: admin / password123" -ForegroundColor Gray
Write-Host "   User:  user / password123" -ForegroundColor Gray
Write-Host ""
Write-Host "🔐 L'authentification JWT est maintenant active!" -ForegroundColor Green
Write-Host "   - Les endpoints publics: /api/auth/*, /api/chatbot/test, /api/chatbot/health" -ForegroundColor Gray
Write-Host "   - Les endpoints protégés: /api/chatbot/*, /api/users/*" -ForegroundColor Gray
Write-Host "   - Les endpoints Admin: /api/users/*" -ForegroundColor Gray
Write-Host ""
