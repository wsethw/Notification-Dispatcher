param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$NodeBaseUrl = "http://localhost:3000",
    [string]$ClientId = "portfolio-client-001"
)

$ErrorActionPreference = "Stop"

Write-Host "Notification Dispatcher - Smoke Tests" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-NotificationRequest {
    param(
        [string]$UserId,
        [string]$Channel,
        [string]$Subject,
        [string]$Body,
        [string]$IdempotencyKey = [guid]::NewGuid().ToString()
    )

    $headers = @{
        "Content-Type" = "application/json"
        "X-Client-Id" = $ClientId
        "Idempotency-Key" = $IdempotencyKey
    }

    $payload = @{
        userId  = $UserId
        channel = $Channel
        subject = $Subject
        body    = $Body
    } | ConvertTo-Json

    return Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/notifications/send" -Headers $headers -Body $payload
}

try {
    $health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/notifications/health"
    Assert-True ($health.status -eq "UP") "Java service health payload is invalid"
    Write-Host "Java service health: $($health.status)" -ForegroundColor Green
}
catch {
    Write-Host "Java service health check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

try {
    $nodeHealth = Invoke-RestMethod -Method Get -Uri "$NodeBaseUrl/api/v1/health"
    Assert-True ($nodeHealth.status -eq "UP") "Node.js service health payload is invalid"
    Write-Host "Node.js service health: $($nodeHealth.status)" -ForegroundColor Green
}
catch {
    Write-Host "Node.js service health check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$idempotencyKey = [guid]::NewGuid().ToString()
$push = Invoke-NotificationRequest -UserId "user-demo" -Channel "push" -Subject "QA Push" -Body "Push notification smoke test" -IdempotencyKey $idempotencyKey
Assert-True (-not [string]::IsNullOrWhiteSpace($push.notificationId)) "Push notification did not return a notificationId"
Write-Host "Push accepted with notificationId: $($push.notificationId)" -ForegroundColor Green

$pushReplay = Invoke-NotificationRequest -UserId "user-demo" -Channel "push" -Subject "QA Push" -Body "Push notification smoke test" -IdempotencyKey $idempotencyKey
Assert-True ($pushReplay.notificationId -eq $push.notificationId) "Idempotency replay returned a different notificationId"
Write-Host "Idempotency replay returned the cached notificationId: $($pushReplay.notificationId)" -ForegroundColor Green

$email = Invoke-NotificationRequest -UserId "user-email-demo" -Channel "email" -Subject "QA Email" -Body "Email notification smoke test"
Assert-True (-not [string]::IsNullOrWhiteSpace($email.notificationId)) "Email notification did not return a notificationId"
Write-Host "Email accepted with notificationId: $($email.notificationId)" -ForegroundColor Green

$sms = Invoke-NotificationRequest -UserId "user-sms-demo" -Channel "sms" -Subject "QA SMS" -Body "SMS notification smoke test"
Assert-True (-not [string]::IsNullOrWhiteSpace($sms.notificationId)) "SMS notification did not return a notificationId"
Write-Host "SMS accepted with notificationId: $($sms.notificationId)" -ForegroundColor Green

$rateLimited = $false
for ($index = 1; $index -le 15; $index++) {
    $headers = @{
        "Content-Type" = "application/json"
        "X-Client-Id" = "rate-limit-test"
        "Idempotency-Key" = [guid]::NewGuid().ToString()
    }

    $payload = @{
        userId  = "user-$index"
        channel = "push"
        subject = "Rate limit test $index"
        body    = "Smoke test request $index"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/v1/notifications/send" -Headers $headers -Body $payload -SkipHttpErrorCheck
    if ($response.StatusCode -eq 429) {
        $rateLimited = $true
        break
    }
}

Assert-True $rateLimited "Expected at least one HTTP 429 during rate limit verification"
Write-Host "Rate limiting produced at least one HTTP 429 as expected" -ForegroundColor Green
