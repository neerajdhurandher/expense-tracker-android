# Android Expense Tracker — Development Plan

> **Purpose**: Kotlin + Jetpack Compose Android app that auto-detects expense SMS, opens an Android Bubble to capture details, saves to Firebase Firestore (per-user via Google Sign-In), and on Home shows a month dropdown with expense list and a category-wise pie chart.

---

## Design Tokens

| Token | Value |
|-------|-------|
| Background | `#1B1A1A` |
| Surface | `#232222` |
| Primary / Accent | `#FFE600` |
| OnPrimary | `#1B1A1A` |
| OnBackground | `#FFFFFF` |
| OnSurfaceVariant | `#B8B8B8` |
| Error | `#FF6B6B` |

Force dark color scheme regardless of system setting.

---

## High-Level Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                        Android Device                             │
├───────────────────────────────────────────────────────────────────┤
│  SMS arrives                                                      │
│       │                                                           │
│       ▼                                                           │
│  SmsReceiver (BroadcastReceiver)                                  │
│       │                                                           │
│       ▼                                                           │
│  SmsParser ──► CategoryClassifier                                 │
│       │                                                           │
│       ▼                                                           │
│  BubbleNotifier ──► Android Bubble (BubbleActivity)               │
│                           │                                       │
│                           ▼                                       │
│                     ExpenseFormSheet (Compose)                    │
│                           │                                       │
│                           ▼                                       │
│                     ExpenseRepository                             │
│                           │                                       │
└───────────────────────────┼───────────────────────────────────────┘
                            │
                            ▼
              ┌─────────────────────────┐
              │      Firebase           │
              │  ┌─────────────────┐    │
              │  │  Auth (Google)  │    │
              │  └─────────────────┘    │
              │  ┌─────────────────┐    │
              │  │   Firestore     │    │
              │  │  (offline OK)   │    │
              │  └─────────────────┘    │
              └─────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 1.9+ |
| UI | Jetpack Compose, Material 3 |
| Architecture | Single-Activity, MVVM |
| DI | Hilt |
| Async | Kotlin Coroutines + StateFlow |
| Auth | Firebase Auth (Google Sign-In via Credential Manager) |
| Database | Firebase Firestore (offline persistence enabled) |
| Charts | MPAndroidChart (PieChart wrapped in `AndroidView`) |
| Permissions | Accompanist-Permissions |
| Image loading | Coil |
| Min SDK | 30 (Android 11 — required for Bubbles API) |
| Target SDK | 34 |

---

## Module / Package Layout

```
com.example.expensetracker
├── app/
│   ├── ExpenseApp.kt              # @HiltAndroidApp
│   └── MainActivity.kt            # Single activity, NavHost
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── auth/
│   │   ├── SignInScreen.kt
│   │   └── AuthViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── graph/
│   │   ├── GraphScreen.kt
│   │   └── GraphViewModel.kt
│   ├── bubble/
│   │   ├── BubbleActivity.kt
│   │   ├── BubbleScreen.kt
│   │   └── BubbleViewModel.kt
│   ├── categories/
│   │   └── ManageCategoriesScreen.kt
│   └── components/
│       └── ExpenseFormSheet.kt    # Shared form (Bubble & manual add)
├── data/
│   ├── model/
│   │   ├── Expense.kt
│   │   ├── Category.kt
│   │   └── ParsedSms.kt
│   ├── repo/
│   │   ├── ExpenseRepository.kt
│   │   ├── CategoryRepository.kt
│   │   └── AuthRepository.kt
│   └── firebase/
│       └── FirestoreSources.kt
├── sms/
│   ├── SmsReceiver.kt
│   ├── SmsParser.kt
│   └── CategoryClassifier.kt
├── notification/
│   └── BubbleNotifier.kt
└── di/
    └── AppModule.kt               # Hilt modules
```

---

## Firestore Data Model

### Collection: `users/{uid}`

```json
{
  "displayName": "string",
  "email": "string",
  "photoUrl": "string?",
  "createdAt": "Timestamp"
}
```

### Sub-collection: `users/{uid}/expenses/{expenseId}`

```json
{
  "name": "string",
  "amount": "number",
  "category": "string",
  "currency": "INR",
  "source": "sms | manual",
  "rawSms": "string?",
  "sender": "string?",
  "occurredAt": "Timestamp",
  "createdAt": "Timestamp",
  "yearMonth": "YYYY-MM"
}
```

### Sub-collection: `users/{uid}/categories/{categoryId}`

```json
{
  "name": "string",
  "color": "string (hex)",
  "isCustom": "boolean",
  "createdAt": "Timestamp"
}
```

### Indexes

| Collection | Fields | Order |
|------------|--------|-------|
| `users/{uid}/expenses` | `yearMonth`, `occurredAt` | ASC, DESC |

### Security Rules (summary)

- Only the authenticated owner can read/write under their `users/{uid}` path.

---

## SMS Parsing Strategy

### Trigger Keywords (case-insensitive)

**Include**: `debited`, `spent`, `paid`, `purchase of`, `txn of`, `sent`

**Exclude**: `credited`, `received`, `refund`, `reversed`, `salary`

### Amount Regex

```regex
(?:INR|Rs\.?|₹)\s?([0-9,]+(?:\.[0-9]{1,2})?)
```

### Merchant Extraction

Capture text after `at `, `to `, `@ `, `towards ` until ` on `, `;`, `.`, `UPI`, or `Ref`.

Fallback: use sender shortcode (e.g., `VK-HDFCBK` → `HDFCBK`).

### Account Masking (sanity check)

```regex
(A/c|Acct|XX|XXXX)\s?\d{2,4}
```

### Output

`ParsedSms(amount, merchant?, sender, rawSms, occurredAt)` or `null` (silently drop non-expense SMS).

---

## Category Classifier — Keyword Map

| Category | Keywords |
|----------|----------|
| Food | SWIGGY, ZOMATO, DOMINO, KFC, MCD, EATSURE |
| Travel | UBER, OLA, IRCTC, RAPIDO, INDIGO, MMT |
| Groceries | BIGBASKET, BLINKIT, ZEPTO, DMART, INSTAMART |
| Shopping | AMAZON, FLIPKART, MYNTRA, AJIO, MEESHO |
| Bills | AIRTEL, JIO, VI, BESCOM, TATAPOWER, BBPS, RECHARGE |
| Entertainment | BOOKMYSHOW, NETFLIX, HOTSTAR, SPOTIFY, PRIME |
| Health | APOLLO, PHARMEASY, 1MG, PRACTO |
| **Other** | *(default fallback)* |

---

## Bubble UX Flow

```
SMS arrives
    │
    ▼
SmsReceiver.onReceive()
    │
    ▼
SmsParser.parse() ──► null? ──► drop
    │
    ▼
CategoryClassifier.classify()
    │
    ▼
BubbleNotifier.show(ParsedSms)
    │
    ├─► NotificationChannel (allowBubbles = true, IMPORTANCE_HIGH)
    ├─► ShortcutInfo (long-lived, CATEGORY_TEXT_SHARE_TARGET)
    └─► Notification with BubbleMetadata
            │
            ▼
        User taps bubble
            │
            ▼
        BubbleActivity (Compose)
            │
            ▼
        ExpenseFormSheet (prefilled)
        ┌─────────────────────────────────┐
        │  Expense Name: [SWIGGY      ]  │
        │  Amount:       [450.00      ]  │
        │  Category:     [Food ▼      ]  │
        │                                 │
        │  ┌─────────────────────────┐   │
        │  │         SAVE            │   │  ← accent #FFE600
        │  └─────────────────────────┘   │
        │         Dismiss                 │
        └─────────────────────────────────┘
            │
            ▼
        ExpenseRepository.add()
            │
            ▼
        Toast + finish() + cancel notification
```

- `BubbleMetadata`: `desiredHeight = 480dp`, `autoExpandBubble = true` (first SMS only).
- If user dismisses without saving: notification auto-cancelled after 10 min via `setTimeoutAfter`.

---

## Home Screen UX

```
┌──────────────────────────────────────┐
│  Expenses                    [👤]   │  ← Profile avatar (menu: Manage Categories, Sign out)
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │  May 2026                   ▼  │  │  ← Month dropdown (last 12 months + "All")
│  └────────────────────────────────┘  │
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │  Total: ₹12,450   •  23 txns   │  │  ← Summary card
│  │  Top: Food                     │  │
│  └────────────────────────────────┘  │
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │  SWIGGY           ₹450        │  │  ← Amount in accent
│  │  Food  •  28 May 2026         │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │  UBER             ₹280        │  │
│  │  Travel  •  27 May 2026       │  │
│  └────────────────────────────────┘  │
│  ...                                 │
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │         VIEW GRAPH             │  │  ← Filled button, accent
│  └────────────────────────────────┘  │
│                               [+]    │  ← FAB for manual add
└──────────────────────────────────────┘
```

- Empty state: centered illustration + "No expenses for this month".
- Swipe-to-delete with undo snackbar.

---

## Graph Screen UX

```
┌──────────────────────────────────────┐
│  ← May 2026 by Category              │
├──────────────────────────────────────┤
│                                      │
│         ┌───────────────┐            │
│        /   Food 35%      \           │
│       │                   │          │  ← PieChart (donut, hole = bg)
│       │     ₹12,450      │           │
│        \   total         /           │
│         └───────────────┘            │
│                                      │
├──────────────────────────────────────┤
│  Food          ₹4,357     35%        │
│  Travel        ₹2,890     23%        │
│  Shopping      ₹2,100     17%        │
│  Groceries     ₹1,500     12%        │
│  Bills         ₹1,103      9%        │
│  Other         ₹500        4%        │
└──────────────────────────────────────┘
```

---

## Permissions & Manifest

### Permissions

| Permission | Purpose | Runtime? |
|------------|---------|----------|
| `RECEIVE_SMS` | Listen for incoming SMS | Yes |
| `READ_SMS` | Read SMS content | Yes |
| `POST_NOTIFICATIONS` | Show bubble notification (Android 13+) | Yes |
| `INTERNET` | Firebase sync | No |
| `ACCESS_NETWORK_STATE` | Offline detection | No |

### Manifest Declarations

```xml
<receiver
    android:name=".sms.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>

<activity
    android:name=".ui.bubble.BubbleActivity"
    android:documentLaunchMode="always"
    android:resizeableActivity="true"
    android:allowEmbedded="true" />
```

---

## Implementation Phases

### Phase 1 — Project Bootstrap

| Task | Description |
|------|-------------|
| 1.1 | Create Android Studio project: Empty Compose Activity, package `com.example.expensetracker`, minSdk 30, targetSdk 34, Kotlin 1.9+, AGP 8+. |
| 1.2 | Add dependencies: Compose BOM, Material3, Navigation-Compose, Hilt, Lifecycle-ViewModel-Compose, Coroutines, Firebase BOM (Auth, Firestore, Analytics), Play-Services-Auth, MPAndroidChart, Accompanist-Permissions, Coil. |
| 1.3 | Add `google-services.json` placeholder + Google Services Gradle plugin. |
| 1.4 | Apply theme tokens in `ui/theme/`. |

### Phase 2 — Auth & Shell

**Depends on**: Phase 1

| Task | Description |
|------|-------------|
| 2.1 | Implement `AuthRepository` (Firebase Auth + Google Sign-In via Credential Manager API). |
| 2.2 | `SignInScreen` — single "Continue with Google" button (accent). |
| 2.3 | `MainActivity` NavHost: `signin` → `home` → `graph` → `manageCategories`; gate via auth state. |
| 2.4 | On first sign-in: create `users/{uid}` doc, seed 8 default categories. |

### Phase 3 — Home, Manual Add, Graph

**Depends on**: Phase 2 | **Parallel with**: Phase 4

| Task | Description |
|------|-------------|
| 3.1 | `ExpenseRepository.observeByMonth(yearMonth)` — Firestore query ordered by `occurredAt desc`. |
| 3.2 | `HomeViewModel` exposes `StateFlow<HomeUiState>` (selected month, list, totals). |
| 3.3 | Build `HomeScreen` with month dropdown, summary card, expense list, FAB, "View Graph" button. |
| 3.4 | Build shared `ExpenseFormSheet` (used by manual add and Bubble). |
| 3.5 | Build `GraphScreen` with `PieChart` AndroidView + category summary list. |
| 3.6 | `ManageCategoriesScreen` — list presets + add custom. |

### Phase 4 — SMS + Bubble Pipeline

**Depends on**: Phase 2 | **Parallel with**: Phase 3

| Task | Description |
|------|-------------|
| 4.1 | `SmsReceiver` registered in manifest; in `onReceive`, decode `SmsMessage[]`, hand to `SmsParser`. |
| 4.2 | `SmsParser` — implement regex rules, unit-test with 15+ sample SMS strings (HDFC/SBI/ICICI/Axis/Paytm/GPay/PhonePe). |
| 4.3 | `CategoryClassifier` — keyword map + unit tests. |
| 4.4 | `BubbleNotifier`: channel with `setAllowBubbles`, long-lived shortcut, `BubbleMetadata` (desiredHeight 480dp). |
| 4.5 | `BubbleActivity` hosts `ExpenseFormSheet` in bubble mode, reads intent extras. |
| 4.6 | On Save: persist via `ExpenseRepository.add()`, show toast, dismiss notification. |
| 4.7 | Runtime permission flow on first launch: explain → request `READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`. |

### Phase 5 — Polish & Hardening

| Task | Description |
|------|-------------|
| 5.1 | Deploy Firestore security rules + composite index. |
| 5.2 | Implement empty/loading/error states across all screens. |
| 5.3 | Add swipe-to-delete with undo snackbar. |
| 5.4 | Integrate Crashlytics + analytics events (`expense_saved`, `sms_parsed`, `bubble_opened`). |
| 5.5 | Write README with Firebase setup steps. |
| 5.6 | Add ProGuard rules for MPAndroidChart and Firebase. |

---

## Files to Create

| File | Purpose |
|------|---------|
| `app/build.gradle.kts` | Plugins, dependencies, signing config |
| `app/src/main/AndroidManifest.xml` | Receiver, activities, permissions |
| `ui/theme/Color.kt` | Palette `#1B1A1A` / `#FFE600` |
| `ui/theme/Theme.kt` | Dark theme, status/nav bar colors |
| `ui/theme/Type.kt` | Typography |
| `ui/auth/SignInScreen.kt` | Google Sign-In UI |
| `ui/auth/AuthViewModel.kt` | Auth state management |
| `ui/home/HomeScreen.kt` | Month dropdown, expense list, View Graph |
| `ui/home/HomeViewModel.kt` | Home state |
| `ui/graph/GraphScreen.kt` | `AndroidView { PieChart(...) }` |
| `ui/graph/GraphViewModel.kt` | Graph data |
| `ui/bubble/BubbleActivity.kt` | Bubble host activity |
| `ui/bubble/BubbleScreen.kt` | Bubble Compose content |
| `ui/bubble/BubbleViewModel.kt` | Bubble state |
| `ui/categories/ManageCategoriesScreen.kt` | Category management |
| `ui/components/ExpenseFormSheet.kt` | Shared form (name, amount, category, Save) |
| `sms/SmsReceiver.kt` | BroadcastReceiver for SMS |
| `sms/SmsParser.kt` | SMS parsing logic |
| `sms/CategoryClassifier.kt` | Keyword-based classification |
| `notification/BubbleNotifier.kt` | Bubble notification builder |
| `data/model/Expense.kt` | Expense data class |
| `data/model/Category.kt` | Category data class |
| `data/model/ParsedSms.kt` | Parsed SMS data class |
| `data/repo/ExpenseRepository.kt` | Firestore expense operations |
| `data/repo/CategoryRepository.kt` | Firestore category operations |
| `data/repo/AuthRepository.kt` | Firebase Auth operations |
| `data/firebase/FirestoreSources.kt` | Collection refs, mappers |
| `di/AppModule.kt` | Hilt modules |
| `firestore.rules` | Security rules |
| `firestore.indexes.json` | Composite indexes |
| `app/src/test/.../SmsParserTest.kt` | Parser unit tests |
| `app/src/test/.../CategoryClassifierTest.kt` | Classifier unit tests |
| `README.md` | Setup instructions |

---

## Verification Checklist

| # | Test | Expected Result |
|---|------|-----------------|
| 1 | `SmsParserTest` | ≥80% coverage on 15+ real-world Indian bank/UPI SMS samples (debits parsed, credits/refunds ignored). |
| 2 | `CategoryClassifierTest` | Every keyword group correctly mapped. |
| 3 | Emulator SMS test | `adb emu sms send VK-HDFCBK "Rs 450 debited from a/c XX1234 at SWIGGY on 12-05-26"` → bubble opens prefilled SWIGGY / 450 / Food → Save → row appears in Home. |
| 4 | Auth flow | Sign in with Google → verify `users/{uid}` doc + seeded categories in Firestore console. |
| 5 | Month filter | Add expenses in two different months → switch dropdown → list updates correctly. |
| 6 | Graph | Pie chart slice values match category totals for selected month. |
| 7 | Offline sync | Airplane mode → Save → row visible (offline cache) → reconnect → Firestore syncs. |
| 8 | Permissions denied | Deny SMS permissions → manual add still works; banner prompts to grant. |
| 9 | Security rules | Firestore emulator test: user A cannot read user B's expenses. |

---

## Sample SMS Test Cases

```
# HDFC Debit
"Rs 450.00 debited from a/c XX1234 on 28-05-26 at SWIGGY. UPI Ref: 123456789012"

# SBI Debit
"Dear Customer, Rs.1200 has been debited from your A/c XXXX5678 on 28/05/2026 to VPA abc@upi. Your UPI transaction reference number is 112345678901."

# ICICI Debit
"ICICI Bank Acct XX123 debited INR 890.50 on 28-May-26; Amazon. UPI:112233445566."

# Paytm
"Paid Rs. 250 to UBER from Paytm Wallet on 28-05-2026. Txn ID: TXN123456"

# GPay
"Sent Rs.150 to PhonePe merchant ZOMATO via UPI on 28/05/26. Ref No 998877665544"

# Credit (should be ignored)
"Rs 5000.00 credited to your a/c XX1234 on 28-05-26 by NEFT. Ref: SALARY-MAY"

# Refund (should be ignored)
"Refund of Rs.450 processed to your card XX9999 for order #12345"
```

---

## Decisions Summary

| Decision | Choice |
|----------|--------|
| Stack | Kotlin + Jetpack Compose + Hilt + Firebase Auth/Firestore + MPAndroidChart |
| Bubble implementation | Official Android Bubbles API |
| Min SDK | 30 (required for Bubbles) |
| Categories | 8 presets seeded on first login + user-added custom |
| Auto-categorization | Keyword rules, default `Other` |
| Offline support | Firestore persistence enabled |
| Currency | INR only (v1) |
| Auth | Google Sign-In via Firebase Auth |

---

## Excluded from v1

- Budget limits / alerts
- Recurring expenses
- Multi-currency support
- CSV / PDF export
- Home screen widgets
- iOS version
- Light theme toggle (dark only)
- Manual SMS import
- Bank account linking

---

## Firebase Setup Steps (for README)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.example.expensetracker`.
3. Add SHA-1 fingerprint (debug + release) for Google Sign-In.
4. Enable **Authentication** → Sign-in method → **Google**.
5. Enable **Cloud Firestore** in Native mode.
6. Download `google-services.json` and place in `app/` folder.
7. Deploy security rules:
   ```
   firebase deploy --only firestore:rules
   ```
8. Deploy indexes:
   ```
   firebase deploy --only firestore:indexes
   ```

---

## Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /expenses/{expenseId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /categories/{categoryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## Firestore Indexes

```json
{
  "indexes": [
    {
      "collectionGroup": "expenses",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "yearMonth", "order": "ASCENDING" },
        { "fieldPath": "occurredAt", "order": "DESCENDING" }
      ]
    }
  ]
}
```
