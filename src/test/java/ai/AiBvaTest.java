package ai;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AiBvaTest {
    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    // valori "neutre"
    private static final boolean NEW_BENEFICIARY = false;
    private static final String COUNTRY = "RO";
    private static final String MERCHANT = "FOOD";
    private static final int TX_COUNT = 1;
    private static final double AVG = 100;
    private static final Set<String> EMPTY_SET = Set.of();

    // --- amount > 0 ---

    @Test
    void amountJustBelowZero_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(-0.01, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void amountAtZero_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(0, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void amountJustAboveZero_shouldPass() {
        assertDoesNotThrow(() ->
                detector.evaluateTransaction(0.01, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    // --- hourOfDay [0, 23] ---

    @Test
    void hourBelowLowerBound_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, -1, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void hourAtLowerBound_shouldPass() {
        assertDoesNotThrow(() ->
                detector.evaluateTransaction(100, 0, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void hourAtUpperBound_shouldPass() {
        assertDoesNotThrow(() ->
                detector.evaluateTransaction(100, 23, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void hourAboveUpperBound_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 24, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET));
    }

    // --- transactionsLast24h >= 0 ---

    @Test
    void transactionsNegative_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, -1, AVG, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void transactionsAtZero_shouldPass() {
        assertDoesNotThrow(() ->
                detector.evaluateTransaction(100, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, 0, AVG, EMPTY_SET, EMPTY_SET));
    }

    // --- averagePreviousAmount >= 0 ---

    @Test
    void avgNegative_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, -0.01, EMPTY_SET, EMPTY_SET));
    }

    @Test
    void avgAtZero_shouldPass() {
        assertDoesNotThrow(() ->
                detector.evaluateTransaction(100, 12, NEW_BENEFICIARY,
                        COUNTRY, MERCHANT, TX_COUNT, 0, EMPTY_SET, EMPTY_SET));
    }

    // --- praguri importante din logică (frontiere scor) ---

    @Test
    void amountAt1000_boundary() {
        // exact 1000 → NU trebuie să adauge +10
        TransactionDecision result = detector.evaluateTransaction(
                1000, 12, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);

        assertEquals(TransactionDecision.APPROVED, result);
    }

    @Test
    void amountJustAbove1000_boundary() {
        TransactionDecision result = detector.evaluateTransaction(
                1000.01, 12, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);

        assertNotEquals(TransactionDecision.APPROVED, result);
    }

    @Test
    void amountAt5000_boundary() {
        TransactionDecision result = detector.evaluateTransaction(
                5000, 12, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);

        // doar +10, nu +20
        assertNotEquals(TransactionDecision.BLOCKED, result);
    }

    @Test
    void amountJustAbove5000_boundary() {
        TransactionDecision result = detector.evaluateTransaction(
                5000.01, 12, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);

        // +10 +20 → risc mai mare
        assertTrue(result == TransactionDecision.REQUIRES_2FA
                || result == TransactionDecision.MANUAL_REVIEW
                || result == TransactionDecision.BLOCKED);
    }

    //    @Test
    //    void hourNightBoundary() {
    //        // 6 NU e noapte
    //        TransactionDecision at6 = detector.evaluateTransaction(
    //                100, 6, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);
    //
    //        // 5 ESTE noapte
    //        TransactionDecision at5 = detector.evaluateTransaction(
    //                100, 5, false, COUNTRY, MERCHANT, TX_COUNT, AVG, EMPTY_SET, EMPTY_SET);
    //
    //        assertNotEquals(at6, at5);
    //    }
}
