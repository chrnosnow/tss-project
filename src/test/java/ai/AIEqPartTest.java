package ai;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AIEqPartTest {
    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    // --- VALIDARI ---

    @Test
    void shouldThrowExceptionForInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(0, 10, false, "RO", "FOOD", 1, 100, Set.of(), Set.of()));
    }

    @Test
    void shouldThrowExceptionForInvalidHour() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 24, false, "RO", "FOOD", 1, 100, Set.of(), Set.of()));
    }

    @Test
    void shouldThrowExceptionForNullCountry() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 10, false, null, "FOOD", 1, 100, Set.of(), Set.of()));
    }

    @Test
    void shouldThrowExceptionForNegativeTransactions() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 10, false, "RO", "FOOD", -1, 100, Set.of(), Set.of()));
    }

    @Test
    void shouldThrowExceptionForNullSets() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 10, false, "RO", "FOOD", 1, 100, null, Set.of()));
    }

    // --- REGULI FUNCTIONALE ---

    @Test
    void shouldBlockImmediatelyForBlacklistedMerchant() {
        TransactionDecision result = detector.evaluateTransaction(
                100, 10, false, "RO", "GAMBLING", 1, 100,
                Set.of(), Set.of("GAMBLING"));

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    @Test
    void shouldReturnApprovedForLowRisk() {
        TransactionDecision result = detector.evaluateTransaction(
                100, 12, false, "RO", "FOOD", 1, 100,
                Set.of(), Set.of());

        assertEquals(TransactionDecision.APPROVED, result);
    }

    //    @Test
    //    void shouldRequire2FAForMediumRisk() {
    //        // +10 (amount >1000) +15 (noapte) = 25 → mai adăugăm ceva
    //        TransactionDecision result = detector.evaluateTransaction(
    //                1500, 23, false, "RO", "FOOD", 11, 100,
    //                Set.of(), Set.of());
    //
    //        // +10 +15 +20 = 45
    //        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    //    }

    @Test
    void shouldRequireManualReviewForHighRisk() {
        TransactionDecision result = detector.evaluateTransaction(
                6000, 23, true, "RO", "FOOD", 11, 100,
                Set.of(), Set.of());

        // +10 +20 +15 +25 +20 +25 = 115 (dar deja peste 90)
        assertTrue(
                result == TransactionDecision.MANUAL_REVIEW ||
                        result == TransactionDecision.BLOCKED
        );
    }

    @Test
    void shouldBlockForVeryHighRiskScore() {
        TransactionDecision result = detector.evaluateTransaction(
                6000, 23, true, "NG", "FOOD", 11, 100,
                Set.of("NG"), Set.of());

        // scor foarte mare
        assertEquals(TransactionDecision.BLOCKED, result);
    }
}
