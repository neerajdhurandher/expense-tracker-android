package com.example.sms

import java.util.Locale

object CategoryClassifier {

    private val KEYWORD_MAP = mapOf(
        "Food" to listOf("SWIGGY", "ZOMATO", "DOMINO", "KFC", "MCD", "EATSURE", "RESTAURANT", "CAFE", "BAKERY", "BURGER", "PIZZA", "FOOD"),
        "Travel" to listOf("UBER", "OLA", "IRCTC", "RAPIDO", "INDIGO", "MMT", "METRO", "CAB", "TAXI", "FLIGHT", "TRAIN", "BUS", "AUTO"),
        "Groceries" to listOf("BIGBASKET", "BLINKIT", "ZEPTO", "DMART", "INSTAMART", "SPENCER", "SUPERMARKET", "GROCERY", "PROVISION", "KIRANA"),
        "Shopping" to listOf("AMAZON", "FLIPKART", "MYNTRA", "AJIO", "MEESHO", "SHOP", "STORE", "MALL", "FASHION", "CLOTH"),
        "Bills" to listOf("AIRTEL", "JIO", "VI", "BESCOM", "TATAPOWER", "BBPS", "RECHARGE", "GAS", "ELECTRICITY", "WATER", "BROADBAND", "INTERNET", "BILL"),
        "Entertainment" to listOf("BOOKMYSHOW", "NETFLIX", "HOTSTAR", "SPOTIFY", "PRIME", "CINEMA", "MOVIE", "PLAY", "TICKET"),
        "Health" to listOf("APOLLO", "PHARMEASY", "1MG", "PRACTO", "MEDICINE", "PHARMACY", "DR", "DENTIST", "HOSPITAL", "CLINIC")
    )

    fun classify(text: String): String {
        val uppercaseText = text.uppercase(Locale.ROOT)
        for ((category, keywords) in KEYWORD_MAP) {
            for (keyword in keywords) {
                if (uppercaseText.contains(keyword)) {
                    return category
                }
            }
        }
        return "Other"
    }
}
