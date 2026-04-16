param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ClientId = "portfolio-client-001"
)

$ErrorActionPreference = "Stop"

Write-Host "Notification Dispatcher - Smoke Tests" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

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
    Write-Host "Java service health: $($health.status)" -ForegroundColor Green
}
catch {
    Write-Host "Java service health check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$push = Invoke-NotificationRequest -UserId "user-demo" -Channel "push" -Subject "QA Push" -Body "Push notification smoke test"
Write-Host "Push accepted with notificationId: $($push.notificationId)" -ForegroundColor Green

$email = Invoke-NotificationRequest -UserId "user-email-demo" -Channel "email" -Subject "QA Email" -Body "Email notification smoke test"
Write-Host "Email accepted with notificationId: $($email.notificationId)" -ForegroundColor Green

$sms = Invoke-NotificationRequest -UserId "user-sms-demo" -Channel "sms" -Subject "QA SMS" -Body "SMS notification smoke test"
Write-Host "SMS accepted with notificationId: $($sms.notificationId)" -ForegroundColor Green
