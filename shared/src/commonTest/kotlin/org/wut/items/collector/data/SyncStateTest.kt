package org.wut.items.collector.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue











class SyncStateTest {

    @Test
    fun `Idle and Syncing are singletons`() {
        
        assertTrue(SyncState.Idle === SyncState.Idle)
        assertTrue(SyncState.Syncing === SyncState.Syncing)
    }

    @Test
    fun `Ok defaults imageUploadFailures to 0`() {
        val ok = SyncState.Ok()
        assertEquals(0, ok.imageUploadFailures)
    }

    @Test
    fun `Ok with same failures count is equal`() {
        assertEquals(SyncState.Ok(0), SyncState.Ok(0))
        assertEquals(SyncState.Ok(3), SyncState.Ok(3))
        assertNotEquals(SyncState.Ok(0), SyncState.Ok(1))
    }

    @Test
    fun `Error stores message`() {
        val err = SyncState.Error("Connection refused")
        assertEquals("Connection refused", err.message)
    }

    @Test
    fun `pattern matching distinguishes all states`() {
        
        fun classify(s: SyncState): String = when (s) {
            SyncState.Idle -> "idle"
            SyncState.Syncing -> "syncing"
            is SyncState.Ok -> if (s.imageUploadFailures == 0) "ok" else "ok-with-failures"
            is SyncState.Error -> "error"
        }

        assertEquals("idle", classify(SyncState.Idle))
        assertEquals("syncing", classify(SyncState.Syncing))
        assertEquals("ok", classify(SyncState.Ok(0)))
        assertEquals("ok-with-failures", classify(SyncState.Ok(2)))
        assertEquals("error", classify(SyncState.Error("oops")))
    }

    @Test
    fun `snackbar trigger logic - hadFailure detection`() {
        
        
        fun hadFailure(s: SyncState): Boolean =
            (s is SyncState.Ok && s.imageUploadFailures > 0) || s is SyncState.Error

        assertEquals(false, hadFailure(SyncState.Idle))
        assertEquals(false, hadFailure(SyncState.Syncing))
        assertEquals(false, hadFailure(SyncState.Ok(0)))
        assertEquals(true, hadFailure(SyncState.Ok(1)))
        assertEquals(true, hadFailure(SyncState.Ok(99)))
        assertEquals(true, hadFailure(SyncState.Error("network down")))
    }
}
