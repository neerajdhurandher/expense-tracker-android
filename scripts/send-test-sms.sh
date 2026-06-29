#!/bin/bash

#
# SYNOPSIS
#     Send test SMS to Android emulator for expense tracking testing.
#
# DESCRIPTION
#     Sends simulated bank SMS messages to the running Android emulator
#     to test the expense auto-detection pipeline.
#
# USAGE
#     # Default test SMS (HDFC debit at Zomato ₹120)
#     ./scripts/send-test-sms.sh
#
#     # Custom amount and merchant
#     ./scripts/send-test-sms.sh -a 255 -m "Uber"
#
#     # Custom category test
#     ./scripts/send-test-sms.sh -a 1200 -m "AMAZON" -s "VD-ICICIB"
#
#     # Full custom message
#     ./scripts/send-test-sms.sh -c "Paid Rs. 350 to ZOMATO from Paytm Wallet on 31-05-2026. Txn ID: TXN789"
#
#     # Send a credit SMS (should be ignored by parser)
#     ./scripts/send-test-sms.sh -p "credit"
#
#     # Send all preset test cases
#     ./scripts/send-test-sms.sh -p "all"
#

# Default values
AMOUNT=120.00
MERCHANT="ZOMATO"
SENDER="VK-HDFCBK"
CUSTOM=""
PRESET=""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--amount)
            AMOUNT="$2"
            shift 2
            ;;
        -m|--merchant)
            MERCHANT="$2"
            shift 2
            ;;
        -s|--sender)
            SENDER="$2"
            shift 2
            ;;
        -c|--custom)
            CUSTOM="$2"
            shift 2
            ;;
        -p|--preset)
            PRESET="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Find ADB
ADB=$(which adb)
if [ -z "$ADB" ]; then
    echo -e "${RED}adb not found. Install Android SDK or add platform-tools to PATH${NC}"
    exit 1
fi

# Check emulator is running
if ! $ADB devices 2>&1 | grep -q "emulator"; then
    echo -e "${RED}No emulator running. Start one from Android Studio first.${NC}"
    exit 1
fi

# Function to send SMS
send_sms() {
    local from=$1
    local body=$2
    echo ""
    echo -e "${CYAN}Sending SMS...${NC}"
    echo -e "${GRAY}   From: $from${NC}"
    echo -e "${GRAY}   Body: $body${NC}"
    $ADB emu sms send "$from" "$body"
    echo -e "${GREEN}   Sent!${NC}"
}

# Preset test cases
declare -A PRESETS_SENDER
declare -A PRESETS_BODY

PRESETS_SENDER[hdfc]="VK-HDFCBK"
PRESETS_BODY[hdfc]="Rs 450.00 debited from a/c XX1234 on 31-05-26 at SWIGGY. UPI Ref: 123456789012"

PRESETS_SENDER[sbi]="VD-SBIBNK"
PRESETS_BODY[sbi]="Dear Customer, Rs.1200 has been debited from your A/c XXXX5678 on 31/05/2026 to VPA abc@upi."

PRESETS_SENDER[icici]="VD-ICICIB"
PRESETS_BODY[icici]="ICICI Bank Acct XX123 debited INR 890.50 on 31-May-26; Amazon. UPI:112233445566."

PRESETS_SENDER[paytm]="VM-PAYTMB"
PRESETS_BODY[paytm]="Paid Rs. 250 to UBER from Paytm Wallet on 31-05-2026. Txn ID: TXN123456"

PRESETS_SENDER[gpay]="BZ-GPAY"
PRESETS_BODY[gpay]="Sent Rs.150 to PhonePe merchant ZOMATO via UPI on 31/05/26. Ref No 998877665544"

PRESETS_SENDER[credit]="VK-HDFCBK"
PRESETS_BODY[credit]="Rs 5000.00 credited to your a/c XX1234 on 31-05-26 by NEFT. Ref: SALARY-MAY"

PRESETS_SENDER[refund]="VD-ICICIB"
PRESETS_BODY[refund]="Refund of Rs.450 processed to your card XX9999 for order #12345"

PRESETS_SENDER[grocery]="VK-HDFCBK"
PRESETS_BODY[grocery]="Rs 899.00 debited from a/c XX1234 on 31-05-26 at BIGBASKET. UPI Ref: 987654321098"

PRESETS_SENDER[bill]="VD-SBIBNK"
PRESETS_BODY[bill]="Rs 599.00 debited from a/c XX5678 on 31-05-26 at AIRTEL RECHARGE. UPI Ref: 112233445566"

PRESETS_SENDER[shopping]="VD-ICICIB"
PRESETS_BODY[shopping]="ICICI Bank Acct XX123 debited INR 2499.00 on 31-May-26; FLIPKART. UPI:998877665544."

# Handle presets
if [ -n "$PRESET" ]; then
    if [ "$PRESET" == "all" ]; then
        echo -e "${YELLOW}Sending ALL preset test SMS messages...${NC}"
        echo -e "${GRAY}   (3 second delay between each)${NC}"

        for key in hdfc sbi icici paytm gpay credit refund grocery bill shopping; do
            echo ""
            echo -e "${YELLOW}--- [$key] ---${NC}"
            send_sms "${PRESETS_SENDER[$key]}" "${PRESETS_BODY[$key]}"
            sleep 3
        done

        echo ""
        echo -e "${GREEN}All 10 test SMS sent!${NC}"
        exit 0
    elif [ -n "${PRESETS_SENDER[$PRESET]}" ]; then
        send_sms "${PRESETS_SENDER[$PRESET]}" "${PRESETS_BODY[$PRESET]}"
        exit 0
    else
        echo -e "${RED}Unknown preset: '$PRESET'${NC}"
        echo -e "${GRAY}   Available: hdfc, sbi, icici, paytm, gpay, credit, refund, grocery, bill, shopping${NC}"
        exit 1
    fi
fi

# Custom full message
if [ -n "$CUSTOM" ]; then
    send_sms "$SENDER" "$CUSTOM"
    exit 0
fi

# Build message from Amount + Merchant
DATE=$(date +"%d-%m-%y")
REF=$((RANDOM * 1000 + RANDOM))
BODY="Rs $AMOUNT debited from a/c XX1234 at $MERCHANT on $DATE. UPI Ref: $REF"

send_sms "$SENDER" "$BODY"

