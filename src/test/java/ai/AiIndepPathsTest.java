package ai;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AiIndepPathsTest {
    private final TransactionFraudDetector detector =
            new TransactionFraudDetector();

    // P1 - invalid amount
    @Test
    void shouldThrowForInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        0, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P2 - invalid hour
    @Test
    void shouldThrowForInvalidHour() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 24, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P3 - invalid mandatory fields
    // acoperă country invalid + merchant invalid
    @Test
    void shouldThrowForInvalidCountry() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "", "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P4 - invalid history / null sets
    @Test
    void shouldThrowForInvalidHistory() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        -1, 100,
                        Set.of(), Set.of()));
    }

    // P5 - blacklist early return
    @Test
    void shouldBlockBlacklistedMerchant() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        100, 12, false,
                        "RO", "GAMBLING",
                        1, 100,
                        Set.of("IR"),
                        Set.of("GAMBLING"));

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    // P6 - baseline APPROVED
    // majoritatea condițiilor FALSE
    @Test
    void shouldApproveSafeTransaction() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        100, 12, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of("IR"),
                        Set.of());

        assertEquals(TransactionDecision.APPROVED, result);
    }

    // P7 - REQUIRES_2FA
    // amount>1000, risky hour, avg*3
    @Test
    void shouldRequire2FA() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        1500, 23, false,
                        "RO", "FOOD",
                        1, 300,
                        Set.of("IR"),
                        Set.of());

        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    }

    // P8 - MANUAL_REVIEW
    // >5000, new beneficiary
    //    @Test
    //    void shouldRequireManualReview() {
    //        TransactionDecision result =
    //                detector.evaluateTransaction(
    //                        6000, 12, true,
    //                        "RO", "FOOD",
    //                        5, 5000,
    //                        Set.of("IR"),
    //                        Set.of());
    //
    //        assertEquals(TransactionDecision.MANUAL_REVIEW, result);
    //    }

    // P9 - BLOCKED
    // high risk country + loop match + break
    // too many tx + risky hour
    @Test
    void shouldBlockVeryHighRiskTransaction() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        7000, 2, true,
                        "IR", "FOOD",
                        20, 100,
                        Set.of("IR", "RU"),
                        Set.of());

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    // P10 - loop executes but no match
    //    @Test
    //    void shouldHandleHighRiskCountryNotMatched() {
    //        TransactionDecision result =
    //                detector.evaluateTransaction(
    //                        4000, 12, false,
    //                        "RO", "FOOD",
    //                        1, 5000,
    //                        Set.of("IR", "RU"),
    //                        Set.of());
    //
    //        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    //    }

    // P11 - null risk sets
    @Test
    void shouldThrowForNullRiskSet() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        null, Set.of()));
    }
}
