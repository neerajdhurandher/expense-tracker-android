package com.example.data.sync

import android.util.Log
import com.example.data.database.CategoryDao
import com.example.data.database.ExpenseDao
import com.example.data.database.PaymentSourceDao
import com.example.data.database.SourceBudgetDao
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource
import com.example.data.model.SourceBudget
import com.example.data.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Core bidirectional sync engine between Room (local) and Firebase Firestore (cloud).
 *
 * Strategy: Offline-first, last-write-wins conflict resolution.
 * Room is the source of truth for UI. Firestore is the cloud sync target.
 */
class SyncEngine(
    private val firestore: FirebaseFirestore,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val paymentSourceDao: PaymentSourceDao,
    private val sourceBudgetDao: SourceBudgetDao
) {
    companion object {
        private const val TAG = "ExpenseTracker.SyncEngine"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val firebaseAuth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    private fun userCollection(uid: String, collection: String) =
        firestore.collection("users").document(uid).collection(collection)

    // ═══════════════════════════════════════════════════════════════
    // Full Sync
    // ═══════════════════════════════════════════════════════════════

    /**
     * Full bidirectional sync: push local changes → pull remote changes.
     * Called on: app start (if online), connectivity restored, manual pull-to-refresh.
     */
    suspend fun performFullSync() {
        val uid = getCurrentUid()
        if (uid == null) {
            Log.w(TAG, "No authenticated user — skipping sync")
            return
        }

        if (_syncState.value is SyncState.Syncing) {
            Log.d(TAG, "Sync already in progress — skipping")
            return
        }

        _syncState.value = SyncState.Syncing
        Log.i(TAG, "══════ Starting full sync for user: $uid ══════")

        try {
            pushPendingChanges(uid)
            pullRemoteChanges(uid)
            _syncState.value = SyncState.Success()
            Log.i(TAG, "══════ Full sync completed successfully ══════")
        } catch (e: Exception) {
            Log.e(TAG, "��═════ Full sync failed ══════", e)
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Push: Local → Firestore
    // ═══════════════════════════════════════════════════════════════

    /**
     * Push all unsynced local changes to Firestore.
     */
    private suspend fun pushPendingChanges(uid: String) {
        pushPendingExpenses(uid)
        pushPendingCategories(uid)
        pushPendingPaymentSources(uid)
        pushPendingBudgets(uid)
    }

    private suspend fun pushPendingExpenses(uid: String) {
        val unsynced = expenseDao.getUnsyncedExpenses()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced expenses")
        for (expense in unsynced) {
            try {
                val docRef = userCollection(uid, "expenses").document(expense.firestoreId)
                if (expense.syncStatus == SyncStatus.DELETED || expense.isDeleted) {
                    docRef.delete().await()
                    expenseDao.hardDeleteById(expense.id)
                    Log.d(TAG, "  Deleted expense: ${expense.firestoreId}")
                } else {
                    docRef.set(expense.toFirestoreMap()).await()
                    expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCED)
                    Log.d(TAG, "  Pushed expense: ${expense.firestoreId} (${expense.name})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "  Failed to push expense ${expense.firestoreId}: ${e.message}")
            }
        }
    }

    private suspend fun pushPendingCategories(uid: String) {
        val unsynced = categoryDao.getUnsyncedCategories()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced categories")
        for (cat in unsynced) {
            try {
                val docRef = userCollection(uid, "categories").document(cat.name)
                if (cat.syncStatus == SyncStatus.DELETED || cat.isDeleted) {
                    docRef.delete().await()
                    categoryDao.hardDeleteByName(cat.name)
                    Log.d(TAG, "  Deleted category: ${cat.name}")
                } else {
                    docRef.set(cat.toFirestoreMap()).await()
                    categoryDao.updateSyncStatus(cat.name, SyncStatus.SYNCED)
                    Log.d(TAG, "  Pushed category: ${cat.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "  Failed to push category ${cat.name}: ${e.message}")
            }
        }
    }

    private suspend fun pushPendingPaymentSources(uid: String) {
        val unsynced = paymentSourceDao.getUnsyncedPaymentSources()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced payment sources")
        for (source in unsynced) {
            try {
                val docRef = userCollection(uid, "payment_sources").document(source.name)
                if (source.syncStatus == SyncStatus.DELETED || source.isDeleted) {
                    docRef.delete().await()
                    paymentSourceDao.hardDeleteByName(source.name)
                    Log.d(TAG, "  Deleted source: ${source.name}")
                } else {
                    docRef.set(source.toFirestoreMap()).await()
                    paymentSourceDao.updateSyncStatus(source.name, SyncStatus.SYNCED)
                    Log.d(TAG, "  Pushed source: ${source.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "  Failed to push source ${source.name}: ${e.message}")
            }
        }
    }

    private suspend fun pushPendingBudgets(uid: String) {
        val unsynced = sourceBudgetDao.getUnsyncedBudgets()
        Log.d(TAG, "Pushing ${unsynced.size} unsynced budgets")
        for (budget in unsynced) {
            try {
                val docId = budget.firestoreDocId()
                val docRef = userCollection(uid, "source_budgets").document(docId)
                if (budget.syncStatus == SyncStatus.DELETED || budget.isDeleted) {
                    docRef.delete().await()
                    sourceBudgetDao.hardDelete(budget.sourceName, budget.yearMonth)
                    Log.d(TAG, "  Deleted budget: $docId")
                } else {
                    docRef.set(budget.toFirestoreMap()).await()
                    sourceBudgetDao.updateSyncStatus(budget.sourceName, budget.yearMonth, SyncStatus.SYNCED)
                    Log.d(TAG, "  Pushed budget: $docId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "  Failed to push budget ${budget.sourceName}/${budget.yearMonth}: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Pull: Firestore → Local (merge)
    // ══════════���════════════════════════════════════════════════════

    private suspend fun pullRemoteChanges(uid: String) {
        pullRemoteExpenses(uid)
        pullRemoteCategories(uid)
        pullRemotePaymentSources(uid)
        pullRemoteBudgets(uid)
    }

    private suspend fun pullRemoteExpenses(uid: String) {
        val snapshot = userCollection(uid, "expenses").get().await()
        val remoteIds = mutableSetOf<String>()
        Log.d(TAG, "Pulling ${snapshot.documents.size} remote expenses")

        for (doc in snapshot.documents) {
            val remote = doc.toExpense() ?: continue
            remoteIds.add(remote.firestoreId)

            val local = expenseDao.getExpenseByFirestoreId(remote.firestoreId)
            if (local == null) {
                // New from cloud
                if (!remote.isDeleted) {
                    expenseDao.insertExpense(remote.copy(syncStatus = SyncStatus.SYNCED))
                    Log.d(TAG, "  Inserted remote expense: ${remote.firestoreId}")
                }
            } else {
                if (local.syncStatus == SyncStatus.SYNCED) {
                    // No local changes — accept remote
                    if (remote.isDeleted) {
                        expenseDao.hardDeleteById(local.id)
                    } else {
                        expenseDao.updateExpense(remote.copy(id = local.id, syncStatus = SyncStatus.SYNCED))
                    }
                } else {
                    // Conflict — last-write-wins
                    if (remote.updatedAt > local.updatedAt) {
                        expenseDao.updateExpense(remote.copy(id = local.id, syncStatus = SyncStatus.SYNCED))
                        Log.d(TAG, "  Conflict resolved (remote wins): ${remote.firestoreId}")
                    }
                    // else: keep local version, will be pushed next time
                }
            }
        }

        // Remove local SYNCED items that no longer exist remotely (deleted on another device)
        val localSynced = expenseDao.getSyncedExpenses()
        for (local in localSynced) {
            if (local.firestoreId !in remoteIds) {
                expenseDao.hardDeleteById(local.id)
                Log.d(TAG, "  Removed locally (deleted remotely): ${local.firestoreId}")
            }
        }
    }

    private suspend fun pullRemoteCategories(uid: String) {
        val snapshot = userCollection(uid, "categories").get().await()
        val remoteNames = mutableSetOf<String>()
        Log.d(TAG, "Pulling ${snapshot.documents.size} remote categories")

        for (doc in snapshot.documents) {
            val remote = doc.toCategory() ?: continue
            remoteNames.add(remote.name)

            val local = categoryDao.getCategoryByName(remote.name)
            if (local == null) {
                if (!remote.isDeleted) {
                    categoryDao.insertCategory(remote.copy(syncStatus = SyncStatus.SYNCED))
                }
            } else {
                if (local.syncStatus == SyncStatus.SYNCED) {
                    if (remote.isDeleted) {
                        categoryDao.hardDeleteByName(local.name)
                    } else {
                        categoryDao.insertCategory(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                } else {
                    if (remote.updatedAt > local.updatedAt) {
                        categoryDao.insertCategory(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        val localSynced = categoryDao.getSyncedCategories()
        for (local in localSynced) {
            if (local.name !in remoteNames) {
                categoryDao.hardDeleteByName(local.name)
            }
        }
    }

    private suspend fun pullRemotePaymentSources(uid: String) {
        val snapshot = userCollection(uid, "payment_sources").get().await()
        val remoteNames = mutableSetOf<String>()
        Log.d(TAG, "Pulling ${snapshot.documents.size} remote payment sources")

        for (doc in snapshot.documents) {
            val remote = doc.toPaymentSource() ?: continue
            remoteNames.add(remote.name)

            val local = paymentSourceDao.getPaymentSourceByName(remote.name)
            if (local == null) {
                if (!remote.isDeleted) {
                    paymentSourceDao.insertPaymentSource(remote.copy(syncStatus = SyncStatus.SYNCED))
                }
            } else {
                if (local.syncStatus == SyncStatus.SYNCED) {
                    if (remote.isDeleted) {
                        paymentSourceDao.hardDeleteByName(local.name)
                    } else {
                        paymentSourceDao.insertPaymentSource(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                } else {
                    if (remote.updatedAt > local.updatedAt) {
                        paymentSourceDao.insertPaymentSource(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        val localSynced = paymentSourceDao.getSyncedPaymentSources()
        for (local in localSynced) {
            if (local.name !in remoteNames) {
                paymentSourceDao.hardDeleteByName(local.name)
            }
        }
    }

    private suspend fun pullRemoteBudgets(uid: String) {
        val snapshot = userCollection(uid, "source_budgets").get().await()
        val remoteKeys = mutableSetOf<String>()
        Log.d(TAG, "Pulling ${snapshot.documents.size} remote budgets")

        for (doc in snapshot.documents) {
            val remote = doc.toSourceBudget() ?: continue
            val key = remote.firestoreDocId()
            remoteKeys.add(key)

            val local = sourceBudgetDao.getBudget(remote.sourceName, remote.yearMonth)
            if (local == null) {
                if (!remote.isDeleted) {
                    sourceBudgetDao.upsertBudget(remote.copy(syncStatus = SyncStatus.SYNCED))
                }
            } else {
                if (local.syncStatus == SyncStatus.SYNCED) {
                    if (remote.isDeleted) {
                        sourceBudgetDao.hardDelete(local.sourceName, local.yearMonth)
                    } else {
                        sourceBudgetDao.upsertBudget(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                } else {
                    if (remote.updatedAt > local.updatedAt) {
                        sourceBudgetDao.upsertBudget(remote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        val localSynced = sourceBudgetDao.getSyncedBudgets()
        for (local in localSynced) {
            val key = local.firestoreDocId()
            if (key !in remoteKeys) {
                sourceBudgetDao.hardDelete(local.sourceName, local.yearMonth)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Immediate Push (per entity — fire and forget)
    // ═══════════════════════════════════════════════════════════════

    /** Push a single expense to Firestore immediately. Fails silently if offline/unauthenticated. */
    suspend fun pushExpenseIfOnline(expense: Expense) {
        val uid = getCurrentUid() ?: return
        try {
            val docRef = userCollection(uid, "expenses").document(expense.firestoreId)
            if (expense.isDeleted) {
                docRef.delete().await()
                expenseDao.hardDeleteById(expense.id)
            } else {
                docRef.set(expense.toFirestoreMap()).await()
                expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Immediate push failed (will retry on sync): ${e.message}")
        }
    }

    /** Push a single category to Firestore immediately. */
    suspend fun pushCategoryIfOnline(category: Category) {
        val uid = getCurrentUid() ?: return
        try {
            val docRef = userCollection(uid, "categories").document(category.name)
            if (category.isDeleted) {
                docRef.delete().await()
                categoryDao.hardDeleteByName(category.name)
            } else {
                docRef.set(category.toFirestoreMap()).await()
                categoryDao.updateSyncStatus(category.name, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Immediate push failed (will retry on sync): ${e.message}")
        }
    }

    /** Push a single payment source to Firestore immediately. */
    suspend fun pushPaymentSourceIfOnline(source: PaymentSource) {
        val uid = getCurrentUid() ?: return
        try {
            val docRef = userCollection(uid, "payment_sources").document(source.name)
            if (source.isDeleted) {
                docRef.delete().await()
                paymentSourceDao.hardDeleteByName(source.name)
            } else {
                docRef.set(source.toFirestoreMap()).await()
                paymentSourceDao.updateSyncStatus(source.name, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Immediate push failed (will retry on sync): ${e.message}")
        }
    }

    /** Push a single budget to Firestore immediately. */
    suspend fun pushBudgetIfOnline(budget: SourceBudget) {
        val uid = getCurrentUid() ?: return
        try {
            val docId = budget.firestoreDocId()
            val docRef = userCollection(uid, "source_budgets").document(docId)
            if (budget.isDeleted) {
                docRef.delete().await()
                sourceBudgetDao.hardDelete(budget.sourceName, budget.yearMonth)
            } else {
                docRef.set(budget.toFirestoreMap()).await()
                sourceBudgetDao.updateSyncStatus(budget.sourceName, budget.yearMonth, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Immediate push failed (will retry on sync): ${e.message}")
        }
    }
}

