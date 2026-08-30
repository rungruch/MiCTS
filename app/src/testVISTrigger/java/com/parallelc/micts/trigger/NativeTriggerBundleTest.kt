package com.parallelc.micts.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeTriggerBundleTest {

    @Test
    fun entryPointZero_omitsOmniEntryPoint() {
        val payload = buildSessionPayload(entryPoint = 0, invocationTimeMs = 12345L)

        assertEquals(12345L, payload.invocationTimeMs)
        assertNull(payload.omniEntryPoint)
    }

    @Test
    fun entryPointOne_includesOmniEntryPoint() {
        val payload = buildSessionPayload(entryPoint = 1, invocationTimeMs = 67890L)

        assertEquals(67890L, payload.invocationTimeMs)
        assertEquals(1, payload.omniEntryPoint)
    }
}
