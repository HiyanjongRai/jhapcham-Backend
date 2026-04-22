# API Endpoint Tests for Jhapcham Backend

$BASE_URL = "http://localhost:8080"
$RESULTS = @()

function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Body,
        [string]$Description,
        [string]$AuthToken
    )
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($AuthToken) {
            $headers["Authorization"] = "Bearer $AuthToken"
        }
        
        $url = "$BASE_URL$Endpoint"
        
        if ($Method -eq "GET") {
            $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $headers -ErrorAction Stop
        } else {
            $bodyJson = $Body | ConvertTo-Json
            $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $headers -Body $bodyJson -ErrorAction Stop
        }
        
        $RESULTS += @{
            Status = "✅ PASS"
            Endpoint = $Endpoint
            Method = $Method
            Description = $Description
            StatusCode = $response.StatusCode
        }
        
        Write-Host "✅ $Method $Endpoint - $Description"
        Write-Host "   Response: $($response.StatusCode)" -ForegroundColor Green
        return $response
    }
    catch {
        $RESULTS += @{
            Status = "❌ FAIL"
            Endpoint = $Endpoint
            Method = $Method
            Description = $Description
            Error = $_.Exception.Message
        }
        
        Write-Host "❌ $Method $Endpoint - $Description" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   JHAPCHAM API ENDPOINT TESTS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Product Variants Endpoints
Write-Host "1️⃣  PRODUCT VARIANTS TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

# Create variant
$variantBody = @{
    sku = "TEST-SHIRT-M-RED"
    size = "M"
    color = "Red"
    capacity = $null
    stockQuantity = 50
    priceModifier = 200
    description = "Test Medium Red Shirt"
    active = $true
}

Test-Endpoint -Method "POST" -Endpoint "/api/products/1/variants" -Body $variantBody -Description "Create Product Variant"

# Get variants
Test-Endpoint -Method "GET" -Endpoint "/api/products/1/variants" -Description "Get Product Variants"

# Get variant by SKU
Test-Endpoint -Method "GET" -Endpoint "/api/products/1/variants/sku/TEST-SHIRT-M-RED" -Description "Get Variant by SKU"

Write-Host ""

# Test 2: Inventory Alerts
Write-Host "2️⃣  INVENTORY ALERTS TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

Test-Endpoint -Method "GET" -Endpoint "/api/inventory-alerts/my-alerts" -Description "Get My Inventory Alerts" -AuthToken "dummy_token"

Test-Endpoint -Method "GET" -Endpoint "/api/inventory-alerts/unacknowledged" -Description "Get Unacknowledged Alerts" -AuthToken "dummy_token"

Write-Host ""

# Test 3: Refund Management
Write-Host "3️⃣  REFUND MANAGEMENT TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

Test-Endpoint -Method "GET" -Endpoint "/api/refunds/my-refunds" -Description "Get My Refunds" -AuthToken "dummy_token"

Test-Endpoint -Method "GET" -Endpoint "/api/refunds/admin/pending" -Description "Get Pending Refunds (Admin)"

Write-Host ""

# Test 4: Loyalty Points
Write-Host "4️⃣  LOYALTY POINTS TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

Test-Endpoint -Method "GET" -Endpoint "/api/loyalty/my-points" -Description "Get My Loyalty Points" -AuthToken "dummy_token"

Write-Host ""

# Test 5: Dispute Resolution
Write-Host "5️⃣  DISPUTE RESOLUTION TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

Test-Endpoint -Method "GET" -Endpoint "/api/disputes/my-disputes" -Description "Get My Disputes" -AuthToken "dummy_token"

Test-Endpoint -Method "GET" -Endpoint "/api/disputes/admin/pending" -Description "Get Pending Disputes (Admin)"

Write-Host ""

# Test 6: SMS Notifications
Write-Host "6️⃣  SMS NOTIFICATIONS TESTS" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow

Test-Endpoint -Method "GET" -Endpoint "/api/sms/preferences" -Description "Get SMS Preferences" -AuthToken "dummy_token"

Test-Endpoint -Method "GET" -Endpoint "/api/sms/history" -Description "Get SMS History" -AuthToken "dummy_token"

Write-Host ""

# Summary Report
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   TEST SUMMARY REPORT" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$passed = ($RESULTS | Where-Object { $_.Status -eq "✅ PASS" } | Measure-Object).Count
$failed = ($RESULTS | Where-Object { $_.Status -eq "❌ FAIL" } | Measure-Object).Count
$total = $RESULTS.Count

Write-Host "Total Tests: $total" -ForegroundColor Cyan
Write-Host "✅ Passed: $passed" -ForegroundColor Green
Write-Host "❌ Failed: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $RESULTS | Where-Object { $_.Status -eq "❌ FAIL" } | ForEach-Object {
        Write-Host "  - $($_.Method) $($_.Endpoint)" -ForegroundColor Red
        Write-Host "    $($_.Error)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
