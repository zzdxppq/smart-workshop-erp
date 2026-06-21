package com.btsheng.erp.core.sync

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ConflictResolverTest {
    @Test fun `no conflict same ts`() {
        val t = LocalDateTime.of(2026, 6, 10, 10, 0, 0)
        assertEquals(0, ConflictResolver.detect(t, t))
    }
    @Test fun `server newer 10s`() {
        val s = LocalDateTime.of(2026, 6, 10, 10, 0, 10)
        val l = LocalDateTime.of(2026, 6, 10, 10, 0, 0)
        assertEquals(1, ConflictResolver.detect(s, l))
    }
    @Test fun `local newer no conflict`() {
        val s = LocalDateTime.of(2026, 6, 10, 10, 0, 0)
        val l = LocalDateTime.of(2026, 6, 10, 10, 0, 10)
        assertEquals(0, ConflictResolver.detect(s, l))
    }
}
