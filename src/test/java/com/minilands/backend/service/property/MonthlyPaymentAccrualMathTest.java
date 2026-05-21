package com.minilands.backend.service.property;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonthlyPaymentAccrualMathTest {

    @Test
    void poolGross_oneDay_equalsMonthlyOver30() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(1, ChronoUnit.DAYS);
        BigDecimal pool = MonthlyPaymentAccrualMath.poolGross(new BigDecimal("30000"), start, end);
        assertEquals(new BigDecimal("1000.00"), pool);
    }

    @Test
    void overlap_respectsEntryAfterWindowStart() {
        Instant winStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant winEnd = Instant.parse("2026-01-11T00:00:00Z");
        Instant entry = Instant.parse("2026-01-06T00:00:00Z");
        BigDecimal days = MonthlyPaymentAccrualMath.eligibleOverlapDays(entry, winEnd, winStart, winEnd);
        assertEquals(new BigDecimal("5.0000"), days.setScale(4, RoundingMode.HALF_UP));
    }

    @Test
    void overlap_zeroWhenWithdrawnBeforeWindow() {
        Instant winStart = Instant.parse("2026-02-01T00:00:00Z");
        Instant winEnd = Instant.parse("2026-02-15T00:00:00Z");
        Instant entry = Instant.parse("2025-01-01T00:00:00Z");
        Instant withdrawn = Instant.parse("2026-01-15T00:00:00Z");
        BigDecimal days = MonthlyPaymentAccrualMath.eligibleOverlapDays(entry, withdrawn, winStart, winEnd);
        assertEquals(0, BigDecimal.ZERO.compareTo(days));
    }

    @Test
    void elapsedDays_throwsWhenEndNotAfterStart() {
        Instant t = Instant.now();
        assertThrows(IllegalArgumentException.class, () -> MonthlyPaymentAccrualMath.elapsedDays(t, t));
    }
}
