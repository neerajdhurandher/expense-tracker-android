package com.example.data.sync

/**
 * Represents the current state of the sync engine.
 */
sealed class SyncState {
    /** No sync operation in progress */
    data object Idle : SyncState()
    /** Sync operation is currently running */
    data object Syncing : SyncState()
    /** Last sync completed successfully */
    data class Success(val timestamp: Long = System.currentTimeMillis()) : SyncState()
    /** Last sync failed with an error */
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncState()
}

