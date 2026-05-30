# AI Agent Guidelines for Expense Tracker

## Architecture Overview

This is an **Android Kotlin app** using Jetpack Compose with MVVM architecture. The app automatically detects expense transactions from bank SMS messages and allows manual expense tracking.

```
SMS arrives → SmsReceiver → SmsParser → CategoryClassifier → BubbleNotifier
                                                                  │
                                                                  ▼
                                                           Android Bubble
                                                                  │
User interaction → HomeViewModel → ExpenseRepository → Room Database
```

**Component boundaries:**
- **data/** - Room entities, DAOs, and repositories (data layer)
- **sms/** - SMS parsing and category classification (domain logic)
- **notification/** - Android bubble notification system
- **ui/** - Compose screens organized by feature (auth, home, graph, categories, bubble)

**Tech stack:** Kotlin 1.9+, Jetpack Compose, Material 3, Room, Navigation-Compose, Coroutines + StateFlow, Accompanist-Permissions, Coil, Robolectric/Roborazzi for testing. Min SDK 30 (required for Bubbles API).

## Build & Run

```bash
# Build (from project root)
./gradlew assembleDebug

# Run unit tests (uses Robolectric - no emulator needed)
./gradlew testDebugUnitTest

# Run screenshot tests (Roborazzi)
./gradlew verifyRoborazziDebug
```

**Setup requirement:** Create `.env` file with `GEMINI_API_KEY=your_key` (see `.env.example`). For local debug builds, remove `signingConfig = signingConfigs.getByName("debugConfig")` from `app/build.gradle.kts`.

## Project Conventions

### ViewModel Pattern
ViewModels require a custom `Factory` class for dependency injection. See `HomeViewModel.Factory`:
```kotlin
class Factory(private val repo: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repo) as T
    }
}
```
Instantiate in Activity: `val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory(app.expenseRepository) }`

### Repository Initialization
Repositories are created in `ExpenseApp.kt` (Application class) and accessed via `(application as ExpenseApp).repositoryName`.

### Room Database
- Database: `AppDatabase` with singleton pattern via `getDatabase(context)`
- Entities: `Expense`, `Category` in `data/model/`
- DAOs return `Flow<List<T>>` for reactive updates
- `yearMonth` field uses format `"YYYY-MM"` for monthly queries

**Expense entity fields:** `id`, `name`, `amount`, `category`, `currency` (default "INR"), `source` ("sms" | "manual"), `rawSms?`, `sender?`, `occurredAt`, `createdAt`, `yearMonth`

**Category entity fields:** `name` (PK), `color` (hex string), `isCustom`, `createdAt`

### Compose UI Patterns
- Use theme colors from `ui/theme/Color.kt`: `DarkBg`, `DarkSurface`, `AccentYellow`, `LightText`, `MutedText`
- Navigation via `NavHost` in `MainActivity.kt` with routes: `"signin"`, `"home"`, `"graph"`, `"categories"`
- Add `testTag("descriptive_tag")` modifier for testable elements
- Bottom sheets use `Dialog` with custom `Card` styling (see `HomeScreen.kt` lines 399-429)

### SMS Parsing Rules
Located in `sms/SmsParser.kt`:
- **Exclude:** credit/refund messages containing `"credited"`, `"received"`, `"refund"`, `"reversed"`, `"salary"`
- **Include:** debit messages containing `"debited"`, `"spent"`, `"paid"`, `"purchase of"`, `"txn of"`, `"sent"`
- Amount regex: `(?i)(?:INR|Rs\.?|₹)\s?([0-9,]+(?:\.[0-9]{1,2})?)`

### Category Classification
`CategoryClassifier.classify(merchant)` uses keyword matching in `KEYWORD_MAP`. Default category is `"Other"`. Add new categories by extending the map.

**Current keyword mappings:**
| Category | Keywords |
|----------|----------|
| Food | SWIGGY, ZOMATO, DOMINO, KFC, MCD, EATSURE, RESTAURANT, CAFE, BAKERY |
| Travel | UBER, OLA, IRCTC, RAPIDO, INDIGO, MMT, METRO, CAB, TAXI, FLIGHT |
| Groceries | BIGBASKET, BLINKIT, ZEPTO, DMART, INSTAMART, SUPERMARKET |
| Shopping | AMAZON, FLIPKART, MYNTRA, AJIO, MEESHO, SHOP, STORE, MALL |
| Bills | AIRTEL, JIO, VI, BESCOM, TATAPOWER, BBPS, RECHARGE, GAS, ELECTRICITY |
| Entertainment | BOOKMYSHOW, NETFLIX, HOTSTAR, SPOTIFY, PRIME, CINEMA, MOVIE |
| Health | APOLLO, PHARMEASY, 1MG, PRACTO, MEDICINE, PHARMACY, HOSPITAL |

## Bubble UX Flow

```
SMS arrives → SmsReceiver.onReceive() → SmsParser.parse()
    │                                        │
    │                              null? → drop silently
    │                                        │
    ▼                                        ▼
CategoryClassifier.classify() ← ─ ─ ParsedSms(amount, merchant, sender, rawSms)
    │
    ▼
BubbleNotifier.show() → NotificationChannel (allowBubbles=true, IMPORTANCE_HIGH)
    │                 → ShortcutInfo (long-lived)
    │                 → Notification with BubbleMetadata (desiredHeight=480dp)
    │
    ▼
User taps bubble → BubbleActivity → ExpenseFormSheet (prefilled)
    │
    ▼
Save → ExpenseRepository.insert() → Toast + dismiss notification
```

## Testing Approach

- **Unit tests** in `src/test/`: Pure JUnit with Robolectric for Android dependencies
- **SMS tests**: Test parsing edge cases in `SmsParserTest.kt` - cover bank-specific formats
- **Screenshot tests**: Roborazzi in `test/screenshots/` - run `recordRoborazziDebug` to update baselines

**Sample SMS test cases** (use these formats when testing):
```
# HDFC Debit
"Rs 450.00 debited from a/c XX1234 on 28-05-26 at SWIGGY. UPI Ref: 123456789012"

# SBI Debit
"Dear Customer, Rs.1200 has been debited from your A/c XXXX5678 on 28/05/2026 to VPA abc@upi."

# ICICI Debit
"ICICI Bank Acct XX123 debited INR 890.50 on 28-May-26; Amazon. UPI:112233445566."

# Paytm
"Paid Rs. 250 to UBER from Paytm Wallet on 28-05-2026. Txn ID: TXN123456"

# GPay
"Sent Rs.150 to PhonePe merchant ZOMATO via UPI on 28/05/26. Ref No 998877665544"

# Credit (should be IGNORED)
"Rs 5000.00 credited to your a/c XX1234 on 28-05-26 by NEFT. Ref: SALARY-MAY"

# Refund (should be IGNORED)
"Refund of Rs.450 processed to your card XX9999 for order #12345"
```

**Emulator SMS test:** `adb emu sms send VK-HDFCBK "Rs 450 debited from a/c XX1234 at SWIGGY on 12-05-26"`

## Key Files Reference

| Purpose | File |
|---------|------|
| App entry & DI | `ExpenseApp.kt` |
| Navigation setup | `MainActivity.kt` |
| Main UI | `ui/home/HomeScreen.kt`, `HomeViewModel.kt` |
| SMS detection | `sms/SmsReceiver.kt`, `SmsParser.kt`, `CategoryClassifier.kt` |
| Bubble notifications | `notification/BubbleNotifier.kt`, `ui/bubble/BubbleActivity.kt` |
| Data models | `data/model/Expense.kt`, `Category.kt`, `ParsedSms.kt` |
| DB schema | `data/database/AppDatabase.kt`, `ExpenseDao.kt`, `CategoryDao.kt` |
| Repositories | `data/repo/ExpenseRepository.kt`, `CategoryRepository.kt`, `AuthRepository.kt` |
| Theme/colors | `ui/theme/Color.kt`, `Theme.kt` |
| Version catalog | `gradle/libs.versions.toml` |

## Permissions

| Permission | Purpose | Runtime? |
|------------|---------|----------|
| `RECEIVE_SMS` | Listen for incoming SMS | Yes |
| `READ_SMS` | Read SMS content | Yes |
| `POST_NOTIFICATIONS` | Show bubble notification (Android 13+) | Yes |
| `INTERNET` | Network operations | No |
| `ACCESS_NETWORK_STATE` | Offline detection | No |

## Excluded from v1

Budget limits, recurring expenses, multi-currency, CSV/PDF export, widgets, iOS, light theme toggle, manual SMS import, bank account linking.

