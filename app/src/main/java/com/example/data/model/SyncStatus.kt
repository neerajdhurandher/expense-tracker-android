package com.example.data.model

/**
 * Sync status constants for tracking entity synchronization state with Firestore.
 */
object SyncStatus {
    /** Entity is in sync with Firestore */
    const val SYNCED = 0
    /** New entity, never synced to Firestore */
    const val PENDING = 1
    /** Modified locally since last sync */
    const val MODIFIED = 2
    /** Deleted locally, pending remote delete */
    const val DELETED = 3
}

