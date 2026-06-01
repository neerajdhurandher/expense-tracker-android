<#
.SYNOPSIS
    Run all debit SMS test cases for expense tracking testing.

.DESCRIPTION
    Sends 10 different bank debit SMS formats to the Android emulator
    to test various parsing scenarios in the expense auto-detection pipeline.

.USAGE
    # Run all test cases with 3 second delay between each
    .\scripts\run-all-debit-tests.ps1

    # Run with custom delay (seconds)
    .\scripts\run-all-debit-tests.ps1 -Delay 5

    # Run a specific test case by number (1-10)
    .\scripts\run-all-debit-tests.ps1 -TestCase 3
#>

param(
    [int]$Delay = 5,
    [int]$TestCase = 0
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SendSmsScript = Join-Path $ScriptDir "send-test-sms.ps1"

# Verify send-test-sms.ps1 exists
if (-not (Test-Path $SendSmsScript)) {
    Write-Host "send-test-sms.ps1 not found at: $SendSmsScript" -ForegroundColor Red
    exit 1
}

# 10 Debit Test Cases - Different banks, formats, merchants, and categories
$TestCases = @(
    @{
        Name = "HDFC Bank - Swiggy (Food)"
        Sender = "VK-HDFCBK"
        Body = "Rs 325.50 debited from a/c XX4521 on 01-06-26 at SWIGGY. UPI Ref: 412789563214"
    },
    @{
        Name = "SBI - Uber (Travel)"
        Sender = "VD-SBIBNK"
        Body = "Dear Customer, Rs.189.00 has been debited from your A/c XXXX7832 on 01/06/2026 to VPA uber@ybl for UBER TRIP."
    },
    @{
        Name = "ICICI Bank - Amazon (Shopping)"
        Sender = "VD-ICICIB"
        Body = "ICICI Bank Acct XX9876 debited INR 1,599.00 on 01-Jun-26; AMAZON INDIA. UPI:563412789012."
    },
    @{
        Name = "Axis Bank - BigBasket (Groceries)"
        Sender = "VM-AXISBK"
        Body = "INR 756.25 spent on your Axis Bank Card XX5544 at BIGBASKET on 01-06-2026. Not you? Call 1800123456"
    },
    @{
        Name = "Kotak Bank - Airtel (Bills)"
        Sender = "VD-KOTKBK"
        Body = "Rs.499.00 debited from Kotak A/c XX3322 on 01/06/26 for AIRTEL RECHARGE VPA airtel@paytm Ref 789456123012"
    },
    @{
        Name = "Paytm - Dominos (Food)"
        Sender = "VM-PAYTMB"
        Body = "Paid Rs. 445 to DOMINOS PIZZA from Paytm Wallet on 01-06-2026. Txn ID: TXN987654321"
    },
    @{
        Name = "Google Pay - BookMyShow (Entertainment)"
        Sender = "BZ-GPAY"
        Body = "Sent Rs.850 to BOOKMYSHOW via UPI on 01/06/26. Ref No 147258369012"
    },
    @{
        Name = "PhonePe - Apollo Pharmacy (Health)"
        Sender = "BX-PPUPI"
        Body = "Payment of Rs 672.00 to APOLLO PHARMACY successful via PhonePe UPI on 01-Jun-2026. TxnID: PPE789456123"
    },
    @{
        Name = "Yes Bank - Flipkart (Shopping)"
        Sender = "VD-YESBK"
        Body = "Txn of Rs 3,299.00 done on Yes Bank card XX8877 at FLIPKART on 01/06/2026. Avl Limit: Rs 46701"
    },
    @{
        Name = "PNB - OLA Cab (Travel)"
        Sender = "VD-PNBANK"
        Body = "Purchase of Rs.234.00 on PNB Debit Card XX6655 at OLA CABS on 01-06-26. SMS BLOCK to 9264092640 if not done by you"
    }
)

function Send-TestSms($sender, $body) {
    & $SendSmsScript -Sender $sender -Custom $body
}

# Run specific test case
if ($TestCase -gt 0) {
    if ($TestCase -gt $TestCases.Count) {
        Write-Host "Invalid test case number. Valid range: 1-$($TestCases.Count)" -ForegroundColor Red
        exit 1
    }
    $test = $TestCases[$TestCase - 1]
    Write-Host ""
    Write-Host "=== Test Case ${TestCase}: $($test.Name) ===" -ForegroundColor Yellow
    Send-TestSms $test.Sender $test.Body
    exit 0
}

# Run all test cases
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DEBIT SMS TEST SUITE (10 Test Cases)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Delay between tests: $Delay seconds" -ForegroundColor Gray
Write-Host ""

$count = 0
foreach ($test in $TestCases) {
    $count++
    Write-Host ""
    Write-Host "[$count/$($TestCases.Count)] $($test.Name)" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor DarkGray
    Send-TestSms $test.Sender $test.Body

    if ($count -lt $TestCases.Count) {
        Write-Host "   Waiting $Delay seconds..." -ForegroundColor Gray
        Start-Sleep -Seconds $Delay
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  All $($TestCases.Count) test cases sent!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Summary of test cases:" -ForegroundColor Cyan
Write-Host "  1. HDFC - Swiggy (Food)"
Write-Host "  2. SBI - Uber (Travel)"
Write-Host "  3. ICICI - Amazon (Shopping)"
Write-Host "  4. Axis - BigBasket (Groceries)"
Write-Host "  5. Kotak - Airtel (Bills)"
Write-Host "  6. Paytm - Dominos (Food)"
Write-Host "  7. GPay - BookMyShow (Entertainment)"
Write-Host "  8. PhonePe - Apollo (Health)"
Write-Host "  9. Yes Bank - Flipkart (Shopping)"
Write-Host " 10. PNB - OLA (Travel)"
Write-Host ""

