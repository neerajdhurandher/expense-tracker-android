package com.example.sms

import org.junit.Assert.*
import org.junit.Test

class CategoryClassifierTest {

    @Test
    fun testClassifyFood() {
        assertEquals("Food", CategoryClassifier.classify("SWIGGY"))
        assertEquals("Food", CategoryClassifier.classify("Zomato Ltd"))
        assertEquals("Food", CategoryClassifier.classify("KFC Store"))
    }

    @Test
    fun testClassifyTravel() {
        assertEquals("Travel", CategoryClassifier.classify("UBER INDIA"))
        assertEquals("Travel", CategoryClassifier.classify("OLA CABS"))
        assertEquals("Travel", CategoryClassifier.classify("IRCTC ticket"))
    }

    @Test
    fun testClassifyShopping() {
        assertEquals("Shopping", CategoryClassifier.classify("AMAZON IN"))
        assertEquals("Shopping", CategoryClassifier.classify("Myntra Order"))
    }

    @Test
    fun testClassifyOther() {
        assertEquals("Other", CategoryClassifier.classify("RANDOM TEXT"))
    }
}
