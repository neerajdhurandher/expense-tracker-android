# Project Details — Expense Tracker

> **Auto-generated reference for AI agents.** Contains full file inventory, signatures, data models, and internal contracts so agents can understand the codebase without reading every file.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Directory Structure](#directory-structure)
3. [Build Configuration](#build-configuration)
4. [Data Models (Entities)](#data-models-entities)
5. [Database & DAOs](#database--daos)
6. [Repositories](#repositories)
7. [Application Entry Point](#application-entry-point)
8. [ViewModels](#viewmodels)
9. [SMS Pipeline](#sms-pipeline)
10. [Notification System](#notification-system)
11. [UI Screens & Components](#ui-screens--components)
12. [Theme System](#theme-system)
13. [Navigation & Routes](#navigation--routes)
14. [Android Manifest](#android-manifest)
15. [Test Files](#test-files)
16. [Scripts](#scripts)
17. [Dependencies (Version Catalog)](#dependencies-version-catalog)
18. [Preset Data](#preset-data)
19. [Key Function Signatures](#key-function-signatures)

---

## Project Overview

| Property | Value |
|----------|-------|
| **Package** | `com.example` |
| **Application ID** | `com.aistudio.expensetracker.ubpxvd` |
| **Language** | Kotlin 2.2+ |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM (ViewModel + Repository + Room) |
| **Min SDK** | 30 |
| **Target SDK** | 36 |
| **DB** | Room (SQLite) — `expense_tracker_db` |
| **Auth** | Local SharedPreferences-based (simulated Google Sign-In) |
| **DI** | Manual via `ExpenseApp` Application class + ViewModel Factory pattern |
| **Theme** | Light professional theme (Indigo accent `#4F46E5`, light gray bg `#F3F4F9`) |

---

## Directory Structure

```
app/src/main/java/com/example/
├── ExpenseApp.kt                          # Application class, repository initialization
├── MainActivity.kt                        # Navigation host, intent handling, permissions
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt                 # Room DB singleton (v3), migrations
│   │   ├── ExpenseDao.kt                  # Expense CRUD + tracked/untracked queries
│   │   ├── CategoryDao.kt                 # Category CRUD
│   │   └── PaymentSourceDao.kt            # PaymentSource CRUD
│   ├── model/
│   │   ├── Expense.kt                     # @Entity — core expense record
│   │   ├── Category.kt                    # @Entity — expense category with color
│   │   ├── PaymentSource.kt               # @Entity — payment source with smartKeywords
│   │   ├── ParsedSms.kt                   # Data class — parsed SMS result (not an entity)
│   │   └── User.kt                        # Data class — user profile (not an entity)
│   └── repo/
│       ├── ExpenseRepository.kt           # Expense data operations
│       ├── CategoryRepository.kt          # Category ops + default seeding
│       ├── PaymentSourceRepository.kt     # Source ops + default seeding
│       └── AuthRepository.kt              # SharedPreferences-based auth state
├── sms/
│   ├── SmsParser.kt                       # SMS parsing + payment source detection
│   ├── CategoryClassifier.kt             # Keyword-based category classification
│   └── SmsReceiver.kt                     # BroadcastReceiver for incoming SMS
├── notification/
│   ├── BubbleNotifier.kt                  # Android Bubble notification (legacy)
│   ├── ExpenseNotifier.kt                 # Heads-up notification with Save/Edit/Skip
│   └── QuickSaveReceiver.kt              # BroadcastReceiver for notification actions
└── ui/
    ├── auth/
    │   ├── SignInScreen.kt                # Sign-in UI with account chooser dialog
    │   └── AuthViewModel.kt              # Auth state management
    ├── home/
    │   ├── HomeScreen.kt                  # Main screen (1428 lines) — expense list, filters, sheets
    │   └── HomeViewModel.kt              # Home state, expense CRUD, filtering
    ├── graph/
    │   └── GraphScreen.kt                 # Analytics — donut chart + category/source breakdown
    ├── categories/
    │   └── ManageCategoriesScreen.kt      # Category & source management with tabs
    ├── bubble/
    │   └── BubbleActivity.kt             # Bubble activity for expense form
    ├── settings/
    │   └── SettingsScreen.kt             # Settings page — profile, categories, sign out
    ├── components/
    │   └── ExpenseFormSheet.kt           # Reusable expense add/edit form
    └── theme/
        ├── Color.kt                       # Color tokens
        ├── Theme.kt                       # Material 3 theme definition
        └── Type.kt                        # Typography

app/src/test/java/com/example/
├── ExampleUnitTest.kt
├── ExampleRobolectricTest.kt
├── GreetingScreenshotTest.kt
└── sms/
    ├── SmsParserTest.kt
    └── CategoryClassifierTest.kt

app/src/test/screenshots/
└── greeting.png

scripts/
├── send-test-sms.ps1                      # Send test SMS to emulator
└── run-all-debit-tests.ps1               # Run 10 debit SMS test cases

docs/
├── payment-source-feature-plan.md
└── untracked-expenses-feature-plan.md
```

---

## Build Configuration

**`app/build.gradle.kts`** — Key settings:

```
plugins: android.application, kotlin.compose, google.devtools.ksp, roborazzi, secrets
namespace: com.example
compileSdk: 36
minSdk: 30
targetSdk: 36
applicationId: com.aistudio.expensetracker.ubpxvd
Java compatibility: 11
buildFeatures: compose = true, buildConfig = true
unitTests: isIncludeAndroidResources = true (for Robolectric)
```

**Signing:**
- `release` — env-based keystore (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`)
- `debugConfig` — `debug.keystore` with standard android debug credentials
- Debug builds use default signing (no explicit config)

**Secrets Plugin:** reads `.env` file for `GEMINI_API_KEY`

**KSP processors:** Room compiler, Moshi codegen

---

## Data Models (Entities)

### Expense (`data/model/Expense.kt`)
```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                          // Merchant/title
    val amount: Double,
    val category: String,                      // Maps to Category.name
    val currency: String = "INR",
    val source: String,                        // "sms" | "manual"
    val rawSms: String? = null,
    val sender: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val yearMonth: String,                     // Format: "yyyy-MM" (e.g. "2026-06")
    val paymentSource: String = "UPI",         // Maps to PaymentSource.name
    val isTracked: Boolean = true              // false = untracked SMS expense pending user action
)
```

### Category (`data/model/Category.kt`)
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,              // e.g. "Food", "Travel"
    val color: String,                         // Hex code (e.g. "#FF6B6B")
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### PaymentSource (`data/model/PaymentSource.kt`)
```kotlin
@Entity(tableName = "payment_sources")
data class PaymentSource(
    @PrimaryKey val name: String,              // e.g. "UPI", "Cash", "Credit Card"
    val color: String,                         // Hex code (e.g. "#4DABF7")
    val smartKeywords: String = "",            // Comma-separated SMS detection keywords
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### ParsedSms (`data/model/ParsedSms.kt`) — NOT a Room entity
```kotlin
data class ParsedSms(
    val amount: Double,
    val merchant: String?,
    val sender: String,
    val rawSms: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val paymentSource: String = "UPI"
)
```

### User (`data/model/User.kt`) — NOT a Room entity
```kotlin
data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)
```

---

## Database & DAOs

### AppDatabase (`data/database/AppDatabase.kt`)
- **Version:** 3
- **Entities:** `Expense`, `Category`, `PaymentSource`
- **Singleton:** `AppDatabase.getDatabase(context)`
- **Migrations:**
  - `MIGRATION_1_2`: Added `paymentSource` column to expenses (default `"UPI"`), created `payment_sources` table
  - `MIGRATION_2_3`: Added `isTracked` column to expenses (default `1`/true)

### ExpenseDao (`data/database/ExpenseDao.kt`)
```kotlin
interface ExpenseDao {
    fun getAllExpenses(): Flow<List<Expense>>                           // All, ordered by occurredAt DESC
    fun getExpensesByMonth(yearMonth: String): Flow<List<Expense>>     // By month
    fun getAllTrackedExpenses(): Flow<List<Expense>>                    // isTracked = 1
    fun getTrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>
    fun getAllUntrackedExpenses(): Flow<List<Expense>>                  // isTracked = 0
    fun getUntrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>
    suspend fun insertExpense(expense: Expense)
    suspend fun insertExpenseAndGetId(expense: Expense): Long          // Returns auto-gen ID
    suspend fun deleteExpenseById(id: Long)
    suspend fun updateExpense(expense: Expense)
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun markAsTracked(id: Long)                                // SET isTracked = 1
}
```

### CategoryDao (`data/database/CategoryDao.kt`)
```kotlin
interface CategoryDao {
    fun getAllCategories(): Flow<List<Category>>                        // ORDER BY name ASC
    suspend fun insertCategory(category: Category)
    suspend fun insertCategories(categories: List<Category>)
    suspend fun deleteCategory(category: Category)
}
```

### PaymentSourceDao (`data/database/PaymentSourceDao.kt`)
```kotlin
interface PaymentSourceDao {
    fun getAllPaymentSources(): Flow<List<PaymentSource>>               // ORDER BY name ASC
    suspend fun getAllPaymentSourcesList(): List<PaymentSource>         // Suspend (non-Flow)
    suspend fun insertPaymentSource(source: PaymentSource)
    suspend fun insertPaymentSources(sources: List<PaymentSource>)
    suspend fun deletePaymentSource(source: PaymentSource)
}
```

---

## Repositories

### ExpenseRepository (`data/repo/ExpenseRepository.kt`)
- Constructor: `ExpenseRepository(expenseDao: ExpenseDao)`
- Exposes: `allExpenses`, `allTrackedExpenses`, `allUntrackedExpenses` (all `Flow<List<Expense>>`)
- Methods: `getExpensesByMonth()`, `getTrackedExpensesByMonth()`, `getUntrackedExpensesByMonth()`, `insertExpense()`, `insertExpenseAndGetId()`, `deleteExpenseById()`, `updateExpense()`, `getExpenseById()`, `markAsTracked()`

### CategoryRepository (`data/repo/CategoryRepository.kt`)
- Constructor: `CategoryRepository(categoryDao: CategoryDao)`
- **Auto-seeds** 8 default categories on first access (if DB empty)
- Exposes: `allCategories: Flow<List<Category>>`
- Methods: `insertCategory()`, `deleteCategory()`

### PaymentSourceRepository (`data/repo/PaymentSourceRepository.kt`)
- Constructor: `PaymentSourceRepository(dao: PaymentSourceDao)`
- **Auto-seeds** 3 default sources on first access (Cash, UPI, Credit Card)
- Exposes: `allPaymentSources: Flow<List<PaymentSource>>`
- Methods: `insertPaymentSource()`, `deletePaymentSource()`

### AuthRepository (`data/repo/AuthRepository.kt`)
- Constructor: `AuthRepository(context: Context)`
- Uses `SharedPreferences("expense_tracker_auth")` for persistence
- Exposes: `currentUser: StateFlow<User?>`
- Methods: `signIn(email, displayName, photoUrl?) → Result<User>`, `updateProfile(displayName, photoUrl?) → Result<User>`, `signOut() → Result<Unit>`
- UID generated as `"user_" + email.hashCode()`

---

## Application Entry Point

### ExpenseApp (`ExpenseApp.kt`)
```kotlin
class ExpenseApp : Application() {
    lateinit var database: AppDatabase
    lateinit var expenseRepository: ExpenseRepository
    lateinit var categoryRepository: CategoryRepository
    lateinit var authRepository: AuthRepository
    lateinit var paymentSourceRepository: PaymentSourceRepository
}
```
- Initializes all repositories in `onCreate()`
- Runs a heartbeat logger (1-minute interval) for debugging process liveness

### Access pattern from Activity:
```kotlin
val app = application as ExpenseApp
app.expenseRepository
app.categoryRepository
app.paymentSourceRepository
app.authRepository
```

---

## ViewModels

### AuthViewModel (`ui/auth/AuthViewModel.kt`)
- Constructor: `AuthViewModel(authRepository: AuthRepository)`
- State: `currentUser: StateFlow<User?>`
- Methods:
  - `signInWithGoogle(email, name, photoUrl?, onResult)`
  - `updateProfile(displayName, photoUrl?, onResult)`
  - `signOut(onResult)`
- Factory: `AuthViewModel.Factory(authRepository)`

### HomeViewModel (`ui/home/HomeViewModel.kt`)
- Constructor: `HomeViewModel(expenseRepository, categoryRepository, paymentSourceRepository)`
- **State:**
  - `availableMonths: List<YearMonthItem>` — last 12 months
  - `selectedMonth: StateFlow<YearMonthItem?>`
  - `historyFilter: StateFlow<HistoryFilter>` — `All | Saved | ByCategory(name) | BySource(name)`
  - `expensesList: StateFlow<List<Expense>>` — tracked, filtered by month + historyFilter
  - `untrackedExpenses: StateFlow<List<Expense>>` — all untracked (no month filter)
  - `categories: StateFlow<List<Category>>`
  - `paymentSources: StateFlow<List<PaymentSource>>`
  - `pendingSmsExpense: StateFlow<PendingSmsExpense?>` — for notification "Edit" flow
- **Methods:**
  - `selectMonth(month)`, `setHistoryFilter(filter)`
  - `addManualExpense(name, amount, category, paymentSource)`
  - `addParsedSmsExpense(name, amount, category, rawSms, sender, occurredAt, paymentSource)`
  - `deleteExpense(expense)`, `undoDeleteExpense()`
  - `updateExpense(expense, name, amount, category, paymentSource)`
  - `confirmExpense(expense)` — mark untracked as tracked
  - `confirmExpenseWithEdits(expense, name, amount, category, paymentSource)` — update + track
  - `dismissUntrackedExpense(expense)` — delete untracked
  - `isCurrentMonth(expense): Boolean`
  - `setPendingSmsExpense(...)`, `clearPendingSmsExpense()`, `savePendingSmsExpense(...)`
  - `addCategory(category)`, `deleteCategory(category)`
  - `addPaymentSource(source)`, `deletePaymentSource(source)`
- Factory: `HomeViewModel.Factory(expenseRepo, categoryRepo, paymentSourceRepo)`

### Supporting Data Classes:
```kotlin
data class YearMonthItem(val queryValue: String, val displayLabel: String)

sealed class HistoryFilter {
    data object All : HistoryFilter()
    data object Saved : HistoryFilter()
    data class ByCategory(val categoryName: String) : HistoryFilter()
    data class BySource(val sourceName: String) : HistoryFilter()
}

data class PendingSmsExpense(
    val name: String, val amount: Double, val category: String,
    val rawSms: String?, val sender: String?, val occurredAt: Long,
    val paymentSource: String = "UPI", val expenseId: Long = -1L
)
```

---

## SMS Pipeline

### SmsParser (`sms/SmsParser.kt`) — `object`
- **Exclude keywords:** `"credited"`, `"received"`, `"refund"`, `"reversed"`, `"salary"`
- **Include keywords:** `"debited"`, `"spent"`, `"paid"`, `"purchase of"`, `"txn of"`, `"sent"`
- **Amount regex:** `(?i)(?:INR|Rs\.?|₹)\s?([0-9,]+(?:\.[0-9]{1,2})?)`
- **Merchant patterns:**
  1. `(?i)(?:at|to|towards|@)\s+([A-Za-z0-9\s\.\-\@]{1,30})`
  2. `;\s*([A-Za-z0-9\s]{1,30})`
- **Merchant terminators:** `" on ", " via ", " from ", " using ", " through ", " UPI ", " Ref "`, etc.
- **Merchant prefix cleanup:** removes `"phonepe merchant "`, `"vpa "` prefixes
- **Fallback merchant:** cleaned sender (part after `-`, uppercased)

```kotlin
fun parse(smsBody: String, sender: String, timestamp: Long): ParsedSms?
fun detectPaymentSource(smsBody: String, sources: List<PaymentSource>): String  // Default: "UPI"
```

### CategoryClassifier (`sms/CategoryClassifier.kt`) — `object`
```kotlin
fun classify(text: String): String  // Returns category name, default "Other"
```
**Keyword map (uppercased matching):**
| Category | Keywords |
|----------|----------|
| Food | SWIGGY, ZOMATO, DOMINO, KFC, MCD, EATSURE, RESTAURANT, CAFE, BAKERY, BURGER, PIZZA, FOOD |
| Travel | UBER, OLA, IRCTC, RAPIDO, INDIGO, MMT, METRO, CAB, TAXI, FLIGHT, TRAIN, BUS, AUTO |
| Groceries | BIGBASKET, BLINKIT, ZEPTO, DMART, INSTAMART, SPENCER, SUPERMARKET, GROCERY, PROVISION, KIRANA |
| Shopping | AMAZON, FLIPKART, MYNTRA, AJIO, MEESHO, SHOP, STORE, MALL, FASHION, CLOTH |
| Bills | AIRTEL, JIO, VI, BESCOM, TATAPOWER, BBPS, RECHARGE, GAS, ELECTRICITY, WATER, BROADBAND, INTERNET, BILL |
| Entertainment | BOOKMYSHOW, NETFLIX, HOTSTAR, SPOTIFY, PRIME, CINEMA, MOVIE, PLAY, TICKET |
| Health | APOLLO, PHARMEASY, 1MG, PRACTO, MEDICINE, PHARMACY, DR, DENTIST, HOSPITAL, CLINIC |

### SmsReceiver (`sms/SmsReceiver.kt`) — `BroadcastReceiver`
- Listens for `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`
- Pipeline per message:
  1. `SmsParser.parse()` → null = skip
  2. `CategoryClassifier.classify()` on merchant
  3. `SmsParser.detectPaymentSource()` using DB-backed sources (via `runBlocking`)
  4. Insert as **untracked** expense (`isTracked = false`) → get ID
  5. `ExpenseNotifier.showExpenseNotification()` with expense ID
- Uses `AppDatabase.getDatabase(context)` directly (not via repository)
- Log tag: `ExpenseTracker.SmsReceiver`

---

## Notification System

### ExpenseNotifier (`notification/ExpenseNotifier.kt`) — `object`
- Channel: `expense_alerts` / "Expense Alerts" / IMPORTANCE_HIGH
- Notification ID: uses `expenseId.toInt()` for uniqueness (fallback `4001`)
- **Actions:**
  - ✅ SAVE → `QuickSaveReceiver` with `ACTION_QUICK_SAVE`
  - ✏️ EDIT → `MainActivity` with `ACTION_EDIT_EXPENSE` (opens pre-filled form)
  - SKIP → `QuickSaveReceiver` with `ACTION_SKIP`
- Constants: `ACTION_QUICK_SAVE`, `ACTION_EDIT_EXPENSE`, `ACTION_SKIP`, `NOTIFICATION_ID = 4001`

```kotlin
fun showExpenseNotification(context, parsedSms, category, expenseId)
```

### QuickSaveReceiver (`notification/QuickSaveReceiver.kt`) — `BroadcastReceiver`
- Handles `ACTION_QUICK_SAVE`: marks expense as tracked via `expenseDao.markAsTracked(id)`, shows toast
- Handles `ACTION_SKIP`: deletes untracked expense via `expenseDao.deleteExpenseById(id)`
- Uses `goAsync()` + `CoroutineScope(Dispatchers.IO)` for async DB work
- Dismisses notification after action

### BubbleNotifier (`notification/BubbleNotifier.kt`) — `object` (legacy)
- Channel: `expense_sms_notifications` / "Expense Alerts" / IMPORTANCE_HIGH + allowBubbles
- Notification ID: fixed `4001`
- Creates: ShortcutInfo, BubbleMetadata (480dp height), MessagingStyle notification
- Target: `BubbleActivity` for bubble expansion
- Actions: QUICK SAVE → `MainActivity` with `QUICK_ADD_EXPENSE`

---

## UI Screens & Components

### SignInScreen (`ui/auth/SignInScreen.kt`) — 355 lines
- **Composable:** `SignInScreen(viewModel, userEmail, onSignInSuccess)`
- Default email: `"shubhamsukla44@gmail.com"`
- Features: decorative canvas background, wallet icon, "Continue with Google" button
- Account chooser: `Dialog` with two options (user email + Guest Tracker)
- Test tags: `app_title_signin`, `google_signin_button`

### HomeScreen (`ui/home/HomeScreen.kt`) — 1428 lines
- **Composable:** `HomeScreen(viewModel, userName, userEmail, onNavigateToGraph, onNavigateToSettings)`
- **Sections:**
  1. Header (title + avatar + profile dropdown menu with single "Settings" item)
  2. Month selector row + "View analytics" button
  3. Summary card (total spent, transaction count, top category)
  4. Untracked expenses section (collapsible, with Confirm/Edit/Dismiss per item)
  5. History header with filter button
  6. Tracked expense list (swipe-to-delete, swipe-to-edit for current month)
  7. Empty state
- **Bottom sheets:** Add expense, Edit expense, SMS edit, Untracked edit, Filter, Month picker
- **Key composables:**
  - `UntrackedExpenseItem` — card with amber accent, 3 action buttons
  - `ExpenseItemRow` — swipeable card with category icon, source, date
  - `MonthPickerSheetContent` — "All Time" + last 12 months
  - `FilterSheetContent` — All, Saved, By Category, By Source
  - `FilterOptionRow` — reusable filter option with expand support
- **Test tags:** `add_expense_fab`, `profile_avatar`, `menu_settings`, `view_graph_button`, `total_amount_text`, `untracked_section_header`, `filter_button`, `expense_item_{id}`, `untracked_item_{id}`, `untracked_confirm_{id}`, `untracked_edit_{id}`, `untracked_dismiss_{id}`

### SettingsScreen (`ui/settings/SettingsScreen.kt`)
- **Composable:** `SettingsScreen(authViewModel, onNavigateBack, onNavigateToCategories, onSignOut)`
- **Sections:**
  1. Profile card (avatar, display name, email, edit button)
  2. Profile edit form (expandable: name field + save/cancel)
  3. General section: "Categories & Sources" row → navigates to ManageCategoriesScreen
  4. Account section: "Sign Out" row (destructive, red-tinted)
  5. Footer: app version
- **Reusable composable:** `SettingsRow(icon, title, subtitle, onClick, testTag)` — extensible for future settings
- **Test tags:** `settings_back_button`, `profile_card`, `profile_edit_button`, `profile_name_input`, `profile_save_button`, `profile_cancel_button`, `settings_categories_row`, `settings_sign_out`

### GraphScreen (`ui/graph/GraphScreen.kt`) — 318 lines
- **Composable:** `GraphScreen(viewModel, onNavigateBack)`
- Toggle: "BY CATEGORY" / "BY SOURCE"
- Animated donut chart (`DonutChart` composable) with center total
- Category/source breakdown list
- Test tags: `back_button_graph`, `donut_chart`
- Data class: `CategoryShare(categoryName, totalAmount, percentage, colorHex)`

### ManageCategoriesScreen (`ui/categories/ManageCategoriesScreen.kt`) — 621 lines
- **Composable:** `ManageCategoriesScreen(viewModel, onNavigateBack)`
- Two tabs: "Categories" / "Sources"
- **Categories tab:** add form (name + color picker) + active list (preset vs custom, delete custom only)
- **Sources tab:** add form (name + smartKeywords + color picker) + active list with keyword display
- 12 color presets available for selection
- Test tags: `back_button_categories`, `category_name_input`, `save_category_btn`, `color_chip_{hex}`, `delete_category_{name}`, `source_name_input`, `source_keywords_input`, `save_source_btn`, `source_color_chip_{hex}`, `delete_source_{name}`

### ExpenseFormSheet (`ui/components/ExpenseFormSheet.kt`) — 391 lines
- **Composable:** `ExpenseFormSheet(initialName, initialAmount, initialCategory, initialPaymentSource, isEditMode, categories, paymentSources, onSave, onDismiss)`
- Fields: name (OutlinedTextField), amount (decimal keyboard), category dropdown, payment source dropdown
- Validation: name required, amount > 0
- SMS disclaimer row shown when `initialAmount != null`
- Title changes: "Log Expense Manually" / "Capture Expense" / "Edit Expense"
- `onSave` signature: `(name: String, amount: Double, category: String, paymentSource: String) -> Unit`
- Test tags: `expense_name_input`, `expense_amount_input`, `expense_dismiss_btn`, `expense_save_btn`

### BubbleActivity (`ui/bubble/BubbleActivity.kt`) — 121 lines
- Standalone `ComponentActivity` for Android Bubble
- Reads intent extras: `amount`, `merchant`, `sender`, `rawSms`, `occurredAt`, `category`, `expenseId`, `notificationId`
- If `expenseId > 0`: updates existing untracked expense + marks tracked
- Else: inserts new expense
- Cancels notification on save, shows toast, finishes activity

---

## Theme System

### Color Tokens (`ui/theme/Color.kt`)
```kotlin
val DarkBg = Color(0xFFF3F4F9)        // Light gray-blue background
val DarkSurface = Color(0xFFFFFFFF)    // Pure white card surfaces
val AccentYellow = Color(0xFF4F46E5)   // Indigo accent (NOTE: named "Yellow" but is Indigo)
val OnAccent = Color(0xFFFFFFFF)       // White text on accent
val LightText = Color(0xFF0F172A)      // Deep slate for primary text
val MutedText = Color(0xFF475569)      // Mid slate for secondary text
val ErrorRed = Color(0xFFEF4444)       // Warning/delete red
val CardBorder = Color(0xFFE2E8F0)     // Soft border color
```

> **Important naming note:** `AccentYellow` is actually **Indigo (#4F46E5)**, not yellow. The variable name is legacy from the original dark theme. All UI code references `AccentYellow` for the primary accent.

### Theme (`ui/theme/Theme.kt`)
- Uses `lightColorScheme` (not dark despite variable names)
- Status bar: light appearance (dark icons)
- Navigation bar: matches background color
- `MyApplicationTheme` composable wraps all content

### Typography (`ui/theme/Type.kt`)
- Only `bodyLarge` customized (16sp, FontFamily.Default)
- Other styles use Material 3 defaults

---

## Navigation & Routes

Defined in `MainActivity.kt` via `NavHost`:

| Route | Screen | Auth Required |
|-------|--------|---------------|
| `"signin"` | `SignInScreen` | No |
| `"home"` | `HomeScreen` | Yes |
| `"graph"` | `GraphScreen` | Yes |
| `"settings"` | `SettingsScreen` | Yes |
| `"categories"` | `ManageCategoriesScreen` | Yes |

- Start destination: `"signin"` if no user, `"home"` if user exists
- Auth-based auto-navigation via `LaunchedEffect(currentUser)`
- Transition: fade in/out (250ms tween)

### Intent Handling in `MainActivity`:
- `QUICK_ADD_EXPENSE` / `ACTION_QUICK_SAVE`: mark existing untracked expense as tracked (or legacy insert)
- `ACTION_EDIT_EXPENSE`: set `pendingSmsExpense` in HomeViewModel → auto-opens edit form

---

## Android Manifest

### Permissions
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Components
| Component | Type | Exported | Notes |
|-----------|------|----------|-------|
| `ExpenseApp` | Application | — | `android:name=".ExpenseApp"` |
| `MainActivity` | Activity | Yes | Launcher, main entry |
| `BubbleActivity` | Activity | Yes | Bubble target, `documentLaunchMode="always"`, `allowEmbedded="true"` |
| `SmsReceiver` | Receiver | Yes | Permission: `BROADCAST_SMS`, priority 999, listens `SMS_RECEIVED` |
| `QuickSaveReceiver` | Receiver | No | Notification action handler |

---

## Test Files

| File | Type | Description |
|------|------|-------------|
| `sms/SmsParserTest.kt` | Unit test | SMS parsing edge cases |
| `sms/CategoryClassifierTest.kt` | Unit test | Category classification |
| `ExampleUnitTest.kt` | Unit test | Basic example |
| `ExampleRobolectricTest.kt` | Robolectric | Android context test |
| `GreetingScreenshotTest.kt` | Screenshot | Roborazzi screenshot test |
| `screenshots/greeting.png` | Baseline | Screenshot baseline image |

**Test commands:**
```bash
./gradlew testDebugUnitTest           # Run unit tests
./gradlew verifyRoborazziDebug        # Verify screenshots
./gradlew recordRoborazziDebug        # Update screenshot baselines
```

---

## Scripts

### `scripts/send-test-sms.ps1`
```powershell
# Parameters: -Amount, -Merchant, -Sender, -Custom, -Preset
# Presets: "hdfc", "sbi", "icici", "paytm", "gpay", "credit", "refund", "all"
# ADB path: C:\Users\ndhurandher\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

### `scripts/run-all-debit-tests.ps1`
```powershell
# Parameters: -Delay (seconds between tests, default 5), -TestCase (specific test 1-10)
# Sends 10 different bank debit SMS formats covering:
# HDFC(Food), SBI(Travel), ICICI(Shopping), Axis(Groceries), Kotak(Bills),
# Paytm(Food), GPay(Entertainment), PhonePe(Health), YesBank(Shopping), PNB(Travel)
```

---

## Dependencies (Version Catalog)

From `gradle/libs.versions.toml`:

| Dependency | Version | Purpose |
|------------|---------|---------|
| AGP | 9.1.1 | Android Gradle Plugin |
| Kotlin | 2.2.10 | Kotlin compiler |
| Compose BOM | 2024.09.00 | Compose version alignment |
| Room | 2.7.0 | Local database |
| Navigation Compose | 2.8.9 | Screen navigation |
| Lifecycle | 2.8.7 | ViewModel, runtime |
| Activity Compose | 1.10.1 | Activity integration |
| Accompanist Permissions | 0.37.3 | Runtime permissions |
| Coil | 2.7.0 | Image loading |
| Retrofit | 2.12.0 | HTTP client |
| OkHttp | 4.10.0 | HTTP layer |
| Moshi | 1.15.2 | JSON parsing |
| Coroutines | 1.10.2 | Async operations |
| KSP | 2.3.5 | Annotation processing |
| Robolectric | 4.16.1 | Unit test Android runtime |
| Roborazzi | 1.59.0 | Screenshot testing |
| Firebase BOM | 34.12.0 | Firebase (currently only BOM imported) |
| Secrets Plugin | 2.0.1 | .env file management |

---

## Preset Data

### Default Categories (seeded by `CategoryRepository`)
| Name | Color | isCustom |
|------|-------|----------|
| Food | #FF6B6B | false |
| Travel | #4DABF7 | false |
| Groceries | #51CF66 | false |
| Shopping | #FCC419 | false |
| Bills | #BE4BDB | false |
| Entertainment | #FF922B | false |
| Health | #20C997 | false |
| Other | #ADB5BD | false |

### Default Payment Sources (seeded by `PaymentSourceRepository`)
| Name | Color | smartKeywords | isCustom |
|------|-------|---------------|----------|
| Cash | #51CF66 | *(empty)* | false |
| UPI | #4DABF7 | upi,vpa,phonepe,gpay,bhim,paytm,razorpay | false |
| Credit Card | #FF6B6B | credit card,cc ,creditcard | false |

### Category Icon Mapping (used in `HomeScreen.kt` and `UntrackedExpenseItem`)
| Category | Icon |
|----------|------|
| Food | `Icons.Default.LunchDining` |
| Travel | `Icons.Default.DirectionsRun` |
| Groceries | `Icons.Default.ShoppingBasket` |
| Shopping | `Icons.Default.LocalMall` |
| Bills | `Icons.Default.FlashOn` |
| Entertainment | `Icons.Default.SportsEsports` |
| Health | `Icons.Default.LocalHospital` |
| Other/default | `Icons.Default.Bookmark` |

---

## Key Function Signatures

### SMS Pipeline
```kotlin
// SmsParser
object SmsParser {
    fun parse(smsBody: String, sender: String, timestamp: Long = System.currentTimeMillis()): ParsedSms?
    fun detectPaymentSource(smsBody: String, sources: List<PaymentSource>): String
}

// CategoryClassifier
object CategoryClassifier {
    fun classify(text: String): String
}
```

### Notification
```kotlin
// ExpenseNotifier
object ExpenseNotifier {
    fun showExpenseNotification(context: Context, parsedSms: ParsedSms, category: String, expenseId: Long = -1L)
}

// BubbleNotifier
object BubbleNotifier {
    fun showBubbleNotification(context: Context, parsedSms: ParsedSms, category: String)
}
```

### UI Components
```kotlin
// ExpenseFormSheet
@Composable
fun ExpenseFormSheet(
    initialName: String = "",
    initialAmount: Double? = null,
    initialCategory: String = "Other",
    initialPaymentSource: String = "UPI",
    isEditMode: Boolean = false,
    categories: List<Category> = emptyList(),
    paymentSources: List<PaymentSource> = emptyList(),
    onSave: (name: String, amount: Double, category: String, paymentSource: String) -> Unit,
    onDismiss: () -> Unit
)

// HomeScreen
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    userEmail: String,
    onNavigateToGraph: (String) -> Unit,
    onNavigateToSettings: () -> Unit
)

// SettingsScreen
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onSignOut: () -> Unit
)

// GraphScreen
@Composable
fun GraphScreen(viewModel: HomeViewModel, onNavigateBack: () -> Unit)

// ManageCategoriesScreen
@Composable
fun ManageCategoriesScreen(viewModel: HomeViewModel, onNavigateBack: () -> Unit)

// SignInScreen
@Composable
fun SignInScreen(viewModel: AuthViewModel, userEmail: String = "...", onSignInSuccess: () -> Unit)
```

---

## Flow Diagrams

### SMS → Expense Flow
```
SMS arrives
  → SmsReceiver.onReceive()
    → SmsParser.parse(body, sender, timestamp)
      → null? → drop silently
      → ParsedSms
    → CategoryClassifier.classify(merchant)
    → SmsParser.detectPaymentSource(body, dbSources)
    → ExpenseDao.insertExpenseAndGetId(untracked expense)
    → ExpenseNotifier.showExpenseNotification(context, parsedSms, category, expenseId)
      → User sees notification with SAVE / EDIT / SKIP
        → SAVE → QuickSaveReceiver → markAsTracked(id)
        → EDIT → MainActivity (ACTION_EDIT_EXPENSE) → HomeViewModel.setPendingSmsExpense() → Form opens
        → SKIP → QuickSaveReceiver → deleteExpenseById(id)
```

### Manual Expense Flow
```
User taps FAB
  → showAddForm = true → ModalBottomSheet with ExpenseFormSheet
    → User fills name, amount, category, source
    → "Save" → HomeViewModel.addManualExpense()
      → ExpenseRepository.insertExpense() → Room DB
```

### Untracked Expense Flow
```
Untracked expenses appear in HomeScreen above History
  → User can:
    → "Confirm" → HomeViewModel.confirmExpense() → markAsTracked(id)
    → "Edit" → ModalBottomSheet with ExpenseFormSheet → confirmExpenseWithEdits()
    → "Dismiss" → HomeViewModel.dismissUntrackedExpense() → deleteExpenseById(id) + Snackbar with Undo
```

