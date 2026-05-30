<#
.SYNOPSIS
    Send test SMS to Android emulator for expense tracking testing.

.DESCRIPTION
    Sends simulated bank SMS messages to the running Android emulator
    to test the expense auto-detection pipeline.

.USAGE
    # Default test SMS (HDFC debit at Swiggy ₹450)
    .\scripts\send-test-sms.ps1

    # Custom amount and merchant
    .\scripts\send-test-sms.ps1 -Amount 255 -Merchant "Uber"

    # Custom category test
    .\scripts\send-test-sms.ps1 -Amount 1200 -Merchant "AMAZON" -Sender "VD-ICICIB"

    # Full custom message
    .\scripts\send-test-sms.ps1 -Custom "Paid Rs. 350 to ZOMATO from Paytm Wallet on 31-05-2026. Txn ID: TXN789"

    # Send a credit SMS (should be ignored by parser)
    .\scripts\send-test-sms.ps1 -Preset "credit"

    # Send all preset test cases
    .\scripts\send-test-sms.ps1 -Preset "all"
#>

param(
    [double]$Amount = 120.00,
    [string]$Merchant = "ZOMATO",
    [string]$Sender = "VK-HDFCBK",
    [string]$Custom = "",
    [string]$Preset = ""
)

$ADB = "C:\Users\ndhurandher\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verify adb exists
if (-not (Test-Path $ADB)) {
    Write-Host "adb not found at: $ADB" -ForegroundColor Red
    exit 1
}

# Check emulator is running
$devices = (& $ADB devices 2>&1) -join "`n"
if ($devices -notmatch "emulator") {
    Write-Host "No emulator running. Start one from Android Studio first." -ForegroundColor Red
    exit 1
}

function Send-Sms($from, $body) {
    Write-Host ""
    Write-Host "Sending SMS..." -ForegroundColor Cyan
    Write-Host "   From: $from" -ForegroundColor Gray
    Write-Host "   Body: $body" -ForegroundColor Gray
    & $ADB emu sms send $from $body
    Write-Host "   Sent!" -ForegroundColor Green
}

# Preset test cases - built individually to avoid PS 5.1 hashtable parsing issues
$Presets = New-Object 'System.Collections.Generic.Dictionary[string,object]'
$Presets.Add("hdfc",     @{ Sender = "VK-HDFCBK"; Body = "Rs 450.00 debited from a/c XX1234 on 31-05-26 at SWIGGY. UPI Ref: 123456789012" })
$Presets.Add("sbi",      @{ Sender = "VD-SBIBNK"; Body = "Dear Customer, Rs.1200 has been debited from your A/c XXXX5678 on 31/05/2026 to VPA abc@upi." })
$Presets.Add("icici",    @{ Sender = "VD-ICICIB"; Body = "ICICI Bank Acct XX123 debited INR 890.50 on 31-May-26; Amazon. UPI:112233445566." })
$Presets.Add("paytm",    @{ Sender = "VM-PAYTMB"; Body = "Paid Rs. 250 to UBER from Paytm Wallet on 31-05-2026. Txn ID: TXN123456" })
$Presets.Add("gpay",     @{ Sender = "BZ-GPAY";   Body = "Sent Rs.150 to PhonePe merchant ZOMATO via UPI on 31/05/26. Ref No 998877665544" })
$Presets.Add("credit",   @{ Sender = "VK-HDFCBK"; Body = "Rs 5000.00 credited to your a/c XX1234 on 31-05-26 by NEFT. Ref: SALARY-MAY" })
$Presets.Add("refund",   @{ Sender = "VD-ICICIB"; Body = "Refund of Rs.450 processed to your card XX9999 for order #12345" })
$Presets.Add("grocery",  @{ Sender = "VK-HDFCBK"; Body = "Rs 899.00 debited from a/c XX1234 on 31-05-26 at BIGBASKET. UPI Ref: 987654321098" })
$Presets.Add("bill",     @{ Sender = "VD-SBIBNK"; Body = "Rs 599.00 debited from a/c XX5678 on 31-05-26 at AIRTEL RECHARGE. UPI Ref: 112233445566" })
$Presets.Add("shopping", @{ Sender = "VD-ICICIB"; Body = "ICICI Bank Acct XX123 debited INR 2499.00 on 31-May-26; FLIPKART. UPI:998877665544." })

# Handle presets
if ($Preset -ne "") {
    if ($Preset -eq "all") {
        Write-Host "Sending ALL preset test SMS messages..." -ForegroundColor Yellow
        Write-Host "   (3 second delay between each)" -ForegroundColor Gray
        foreach ($key in ($Presets.Keys | Sort-Object)) {
            $p = $Presets[$key]
            Write-Host ""
            Write-Host "--- [$key] ---" -ForegroundColor Yellow
            Send-Sms $p.Sender $p.Body
            Start-Sleep -Seconds 3
        }
        Write-Host ""
        Write-Host "All $($Presets.Count) test SMS sent!" -ForegroundColor Green
        exit 0
    }
    elseif ($Presets.ContainsKey($Preset)) {
        $p = $Presets[$Preset]
        Send-Sms $p.Sender $p.Body
        exit 0
    }
    else {
        Write-Host "Unknown preset: '$Preset'" -ForegroundColor Red
        Write-Host "   Available: $($Presets.Keys -join ', ')" -ForegroundColor Gray
        exit 1
    }
}

# Custom full message
if ($Custom -ne "") {
    Send-Sms $Sender $Custom
    exit 0
}

# Build message from Amount + Merchant
$date = Get-Date -Format "dd-MM-yy"
$ref = Get-Random -Minimum 100000000 -Maximum 999999999
$body = "Rs $Amount debited from a/c XX1234 at $Merchant on $date. UPI Ref: $ref"

Send-Sms $Sender $body
