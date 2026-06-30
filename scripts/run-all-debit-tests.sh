#!/bin/bash

#
# SYNOPSIS
#     Run all debit SMS test cases for expense tracking testing.
#
# DESCRIPTION
#     Sends 10 different bank debit SMS formats to the Android emulator
#     to test various parsing scenarios in the expense auto-detection pipeline.
#
# USAGE
#     # Run all test cases with 5 second delay between each
#     ./scripts/run-all-debit-tests.sh
#
#     # Run with custom delay (seconds)
#     ./scripts/run-all-debit-tests.sh --delay 3
#
#     # Run a specific test case by number (1-10)
#     ./scripts/run-all-debit-tests.sh --test 3
#

# Default values
DELAY=5
TEST_CASE=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
DARK_GRAY='\033[1;30m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SEND_SMS_SCRIPT="$SCRIPT_DIR/send-test-sms.sh"

# Verify send-test-sms.sh exists
if [ ! -f "$SEND_SMS_SCRIPT" ]; then
    echo -e "${RED}send-test-sms.sh not found at: $SEND_SMS_SCRIPT${NC}"
    exit 1
fi

# Make it executable
chmod +x "$SEND_SMS_SCRIPT"

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --delay)
            DELAY="$2"
            shift 2
            ;;
        --test)
            TEST_CASE="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--delay SECONDS] [--test CASE_NUMBER]"
            exit 1
            ;;
    esac
done

# Test cases array
declare -a TEST_NAMES
declare -a TEST_SENDERS
declare -a TEST_BODIES

TEST_NAMES[0]="HDFC Bank - Swiggy (Food)"
TEST_SENDERS[0]="VK-HDFCBK"
TEST_BODIES[0]="Rs 325.50 debited from a/c XX4521 on 01-06-26 at SWIGGY. UPI Ref: 412789563214"

TEST_NAMES[1]="SBI - Uber (Travel)"
TEST_SENDERS[1]="VD-SBIBNK"
TEST_BODIES[1]="Dear Customer, Rs.189.00 has been debited from your A/c XXXX7832 on 01/06/2026 to VPA uber@ybl for UBER TRIP."

TEST_NAMES[2]="ICICI Bank - Amazon (Shopping)"
TEST_SENDERS[2]="VD-ICICIB"
TEST_BODIES[2]="ICICI Bank Acct XX9876 debited INR 1,599.00 on 01-Jun-26; AMAZON INDIA. UPI:563412789012."

TEST_NAMES[3]="Axis Bank - BigBasket (Groceries)"
TEST_SENDERS[3]="VM-AXISBK"
TEST_BODIES[3]="INR 756.25 spent on your Axis Bank Card XX5544 at BIGBASKET on 01-06-2026. Not you? Call 1800123456"

TEST_NAMES[4]="Kotak Bank - Airtel (Bills)"
TEST_SENDERS[4]="VD-KOTKBK"
TEST_BODIES[4]="Rs.499.00 debited from Kotak A/c XX3322 on 01/06/26 for AIRTEL RECHARGE VPA airtel@paytm Ref 789456123012"

TEST_NAMES[5]="Paytm - Dominos (Food)"
TEST_SENDERS[5]="VM-PAYTMB"
TEST_BODIES[5]="Paid Rs. 445 to DOMINOS PIZZA from Paytm Wallet on 01-06-2026. Txn ID: TXN987654321"

TEST_NAMES[6]="Google Pay - BookMyShow (Entertainment)"
TEST_SENDERS[6]="BZ-GPAY"
TEST_BODIES[6]="Sent Rs.850 to BOOKMYSHOW via UPI on 01/06/26. Ref No 147258369012"

TEST_NAMES[7]="PhonePe - Apollo Pharmacy (Health)"
TEST_SENDERS[7]="BX-PPUPI"
TEST_BODIES[7]="Payment of Rs 672.00 to APOLLO PHARMACY successful via PhonePe UPI on 01-Jun-2026. TxnID: PPE789456123"

TEST_NAMES[8]="Yes Bank - Flipkart (Shopping)"
TEST_SENDERS[8]="VD-YESBK"
TEST_BODIES[8]="Txn of Rs 3,299.00 done on Yes Bank card XX8877 at FLIPKART on 01/06/2026. Avl Limit: Rs 46701"

TEST_NAMES[9]="PNB - OLA Cab (Travel)"
TEST_SENDERS[9]="VD-PNBANK"
TEST_BODIES[9]="Purchase of Rs.234.00 on PNB Debit Card XX6655 at OLA CABS on 01-06-26. SMS BLOCK to 9264092640 if not done by you"

TOTAL_TESTS=${#TEST_NAMES[@]}

# Function to send SMS
send_test_sms() {
    local sender=$1
    local body=$2
    "$SEND_SMS_SCRIPT" -s "$sender" -c "$body"
}

# Run specific test case
if [ "$TEST_CASE" -gt 0 ]; then
    if [ "$TEST_CASE" -gt "$TOTAL_TESTS" ]; then
        echo -e "${RED}Invalid test case number. Valid range: 1-$TOTAL_TESTS${NC}"
        exit 1
    fi

    INDEX=$((TEST_CASE - 1))
    echo ""
    echo -e "${YELLOW}=== Test Case $TEST_CASE: ${TEST_NAMES[$INDEX]} ===${NC}"
    send_test_sms "${TEST_SENDERS[$INDEX]}" "${TEST_BODIES[$INDEX]}"
    exit 0
fi

# Run all test cases
echo ""
echo -e "${CYAN}========================================"
echo "  DEBIT SMS TEST SUITE ($TOTAL_TESTS Test Cases)"
echo "========================================${NC}"
echo -e "${GRAY}Delay between tests: $DELAY seconds${NC}"
echo ""

for ((i=0; i<TOTAL_TESTS; i++)); do
    COUNT=$((i + 1))
    echo ""
    echo -e "${YELLOW}[$COUNT/$TOTAL_TESTS] ${TEST_NAMES[$i]}${NC}"
    echo -e "${DARK_GRAY}----------------------------------------${NC}"
    send_test_sms "${TEST_SENDERS[$i]}" "${TEST_BODIES[$i]}"

    if [ $i -lt $((TOTAL_TESTS - 1)) ]; then
        echo -e "${GRAY}   Waiting $DELAY seconds...${NC}"
        sleep "$DELAY"
    fi
done

echo ""
echo -e "${GREEN}========================================"
echo "  All $TOTAL_TESTS test cases sent!"
echo "========================================${NC}"
echo ""
echo -e "${CYAN}Summary of test cases:${NC}"
echo "  1. HDFC - Swiggy (Food)"
echo "  2. SBI - Uber (Travel)"
echo "  3. ICICI - Amazon (Shopping)"
echo "  4. Axis - BigBasket (Groceries)"
echo "  5. Kotak - Airtel (Bills)"
echo "  6. Paytm - Dominos (Food)"
echo "  7. GPay - BookMyShow (Entertainment)"
echo "  8. PhonePe - Apollo (Health)"
echo "  9. Yes Bank - Flipkart (Shopping)"
echo " 10. PNB - OLA (Travel)"
echo ""

