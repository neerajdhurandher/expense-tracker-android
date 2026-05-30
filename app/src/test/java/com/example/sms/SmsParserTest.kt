package com.example.sms

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testHdfcDebit() {
        val sms = "Rs 450.00 debited from a/c XX1234 on 28-05-26 at SWIGGY. UPI Ref: 123456789012"
        val parsed = SmsParser.parse(sms, "VK-HDFCBK")
        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.001)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals("VK-HDFCBK", parsed.sender)
    }

    @Test
    fun testSbiDebit() {
        val sms = "Dear Customer, Rs.1200 has been debited from your A/c XXXX5678 on 28/05/2026 to VPA abc@upi. Your UPI transaction reference number is 112345678901."
        val parsed = SmsParser.parse(sms, "SBI-SMS")
        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.001)
        assertEquals("abc@upi", parsed.merchant)
    }

    @Test
    fun testIciciDebit() {
        val sms = "ICICI Bank Acct XX123 debited INR 890.50 on 28-May-26; Amazon. UPI:112233445566."
        val parsed = SmsParser.parse(sms, "ICICI-BK")
        assertNotNull(parsed)
        assertEquals(890.50, parsed!!.amount, 0.001)
        assertEquals("Amazon", parsed.merchant)
    }

    @Test
    fun testPaytm() {
        val sms = "Paid Rs. 250 to UBER from Paytm Wallet on 28-05-2026. Txn ID: TXN123456"
        val parsed = SmsParser.parse(sms, "PAYTM-WL")
        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals("UBER", parsed.merchant)
    }

    @Test
    fun testGpay() {
        val sms = "Sent Rs.150 to PhonePe merchant ZOMATO via UPI on 28/05/26. Ref No 998877665544"
        val parsed = SmsParser.parse(sms, "GPAY-UPI")
        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount, 0.001)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun testCreditAndRefundIgnored() {
        val creditSms = "Rs 5000.00 credited to your a/c XX1234 on 28-05-26 by NEFT. Ref: SALARY-MAY"
        assertNull(SmsParser.parse(creditSms, "HDFCBK"))

        val refundSms = "Refund of Rs.450 processed to your card XX9999 for order #12345"
        assertNull(SmsParser.parse(refundSms, "HDFCBK"))
    }
}
