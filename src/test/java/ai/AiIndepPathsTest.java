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

    // P1
    @Test
    void invalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        0, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P2
    @Test
    void invalidHour() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 24, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P3
    @Test
    void invalidCountry() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        null, "FOOD",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P4
    @Test
    void invalidMerchant() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "",
                        1, 100,
                        Set.of(), Set.of()));
    }

    // P5
    @Test
    void invalidHistory() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        -1, 100,
                        Set.of(), Set.of()));
    }

    // P6
    @Test
    void nullRiskSet() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        null, Set.of()));
    }

    // P7 - blacklist path
    @Test
    void blacklistedMerchant() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "GAMBLING",
                        1, 100,
                        Set.of(),
                        Set.of("GAMBLING")));
    }

    // P8 - approved baseline
    @Test
    void approvedTransaction() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100, 12, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(),
                        Set.of()));
    }

    // P9 - amount > 1000
    @Test
    void amountAbove1000() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        1500, 12, false,
                        "RO", "FOOD",
                        1, 1000,
                        Set.of(),
                        Set.of()));
    }

    // P10 - amount > 5000
    @Test
    void amountAbove5000() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        6000, 12, false,
                        "RO", "FOOD",
                        1, 5000,
                        Set.of(),
                        Set.of()));
    }

    // P11 - risky hour
    @Test
    void riskyHour() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100, 23, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(),
                        Set.of()));
    }

    // P12 - new beneficiary
    @Test
    void newBeneficiary() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        4000, 12, true,
                        "RO", "FOOD",
                        1, 4000,
                        Set.of(),
                        Set.of()));
    }

    // P13 - loop entered but no match
    //    @Test
    //    void highRiskLoopNoMatch() {
    //        assertEquals(TransactionDecision.REQUIRES_2FA,
    //                detector.evaluateTransaction(
    //                        4000, 12, false,
    //                        "RO", "FOOD",
    //                        1, 4000,
    //                        Set.of("IR", "RU"),
    //                        Set.of()));
    //    }

    // P14 - loop match + break
    //    @Test
    //    void highRiskCountryMatched() {
    //        assertEquals(TransactionDecision.MANUAL_REVIEW,
    //                detector.evaluateTransaction(
    //                        4000, 12, false,
    //                        "IR", "FOOD",
    //                        1, 4000,
    //                        Set.of("IR"),
    //                        Set.of()));
    //    }

    // P15 - transactions > 10
    //    @Test
    //    void tooManyTransactions() {
    //        assertEquals(TransactionDecision.REQUIRES_2FA,
    //                detector.evaluateTransaction(
    //                        100, 12, false,
    //                        "RO", "FOOD",
    //                        11, 100,
    //                        Set.of(),
    //                        Set.of()));
    //    }

    // P16 - anomalous amount
    //    @Test
    //    void anomalousAmount() {
    //        assertEquals(TransactionDecision.REQUIRES_2FA,
    //                detector.evaluateTransaction(
    //                        1000, 12, false,
    //                        "RO", "FOOD",
    //                        1, 200,
    //                        Set.of(),
    //                        Set.of()));
    //    }

    // P17 - APPROVED branch
    @Test
    void approvedDecision() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        50, 12, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(),
                        Set.of()));
    }

    // P18 - 2FA branch
    //    @Test
    //    void requires2FADecision() {
    //        assertEquals(TransactionDecision.REQUIRES_2FA,
    //                detector.evaluateTransaction(
    //                        1500, 23, false,
    //                        "RO", "FOOD",
    //                        1, 1000,
    //                        Set.of(),
    //                        Set.of()));
    //    }

    // P19 - MANUAL_REVIEW branch
    //    @Test
    //    void manualReviewDecision() {
    //        assertEquals(TransactionDecision.MANUAL_REVIEW,
    //                detector.evaluateTransaction(
    //                        6000, 23, true,
    //                        "RO", "FOOD",
    //                        5, 1000,
    //                        Set.of(),
    //                        Set.of()));
    //    }

    // P20 - BLOCKED branch
    @Test
    void blockedDecision() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(
                        7000, 2, true,
                        "IR", "FOOD",
                        20, 100,
                        Set.of("IR"),
                        Set.of()));
    }
}
