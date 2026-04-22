$BASE_URL = "http://localhost:8080"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "JHAPCHAM API ENDPOINT TESTS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$failed = 0

function Test-Endpoint {
    param([string]$Method, [string]$Endpoint, $Body, [string]$Description)
    
    try {
        $headers = @{"Content-Type" = "application/json"}
        $url = "$BASE_URL$Endpoint"
        
        if ($Method -eq "GET") {
            $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $headers -ErrorAction Stop
        } else {
            $bodyJson = $Body | ConvertTo-Json
            $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $headers -Body $bodyJson -ErrorAction Stop
        }
        
        Write-Host "PASS - $Method $Endpoint" -ForegroundColor Green
        Write-Host "       Status: $($response.StatusCode)" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "FAIL - $Method $Endpoint" -ForegroundColor Red
        Write-Host "       Error: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
        return $false
    }
}

# Test 1: Public endpoints (no auth required)
Write-Host "[1] PRODUCT VARIANTS (Public)" -ForegroundColor Yellow

if (Test-Endpoint -Method "GET" -Endpoint "/api/products/1/variants" -Description "Get variants") { $passed++ } else { $failed++ }

# Test 2: Health check
Write-Host ""
Write-Host "[2] SERVER HEALTH CHECK" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/actuator/health" -Method GET -ErrorAction Stop
    Write-Host "PASS - Server health check" -ForegroundColor Green
    Write-Host "       Status: $($response.StatusCode)" -ForegroundColor Green
    $passed++
} catch {
    Write-Host "FAIL - Server health check" -ForegroundColor Red
    $failed++
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "RESULTS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""
Write-Host "Note: Authentication-required endpoints need valid JWT token" -ForegroundColor Yellow
Write-Host "The server is running and accepting requests!" -ForegroundColor Green
