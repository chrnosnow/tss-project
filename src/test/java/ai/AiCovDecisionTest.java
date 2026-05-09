package ai;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AiCovDecisionTest {
    private final TransactionFraudDetector detector =
            new TransactionFraudDetector();

    // amount <= 0  -> true
    @Test
    void invalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        0, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()
                ));
    }

    // hour invalid -> true
    @Test
    void invalidHour() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 24, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of(), Set.of()
                ));
    }

    // country invalid -> true
    @Test
    void invalidCountry() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "", "FOOD",
                        1, 100,
                        Set.of(), Set.of()
                ));
    }

    // merchant invalid -> true
    @Test
    void invalidMerchant() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "",
                        1, 100,
                        Set.of(), Set.of()
                ));
    }

    // negative history values -> true
    @Test
    void negativeHistoryValues() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        -1, 100,
                        Set.of(), Set.of()
                ));
    }

    // null sets -> true
    @Test
    void nullRiskSets() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "FOOD",
                        1, 100,
                        null, Set.of()
                ));
    }

    // blacklisted merchant -> true
    @Test
    void blacklistedMerchant() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        100, 10, false,
                        "RO", "GAMBLING",
                        1, 100,
                        Set.of(),
                        Set.of("GAMBLING")
                );

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    // APPROVED + multe ramuri false
    @Test
    void approvedTransaction() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        100, 12, false,
                        "RO", "FOOD",
                        1, 100,
                        Set.of("IR"),
                        Set.of()
                );

        assertEquals(TransactionDecision.APPROVED, result);
    }

    // REQUIRES_2FA
    //    @Test
    //    void requires2FA() {
    //        TransactionDecision result =
    //                detector.evaluateTransaction(
    //                        1500, 23, false,
    //                        "RO", "FOOD",
    //                        1, 1000,
    //                        Set.of("IR"),
    //                        Set.of()
    //                );
    //
    //        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    //    }

    // MANUAL_REVIEW
    //    @Test
    //    void manualReview() {
    //        TransactionDecision result =
    //                detector.evaluateTransaction(
    //                        6000, 23, true,
    //                        "RO", "FOOD",
    //                        5, 1000,
    //                        Set.of("IR"),
    //                        Set.of()
    //                );
    //
    //        assertEquals(TransactionDecision.MANUAL_REVIEW, result);
    //    }

    // BLOCKED + highRiskCountry found in loop
    @Test
    void blockedHighRiskTransaction() {
        TransactionDecision result =
                detector.evaluateTransaction(
                        7000, 2, true,
                        "IR", "FOOD",
                        20, 100,
                        Set.of("IR"),
                        Set.of()
                );

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    // loop executes but country NOT found
    //    @Test
    //    void highRiskCountryNotMatched() {
    //        TransactionDecision result =
    //                detector.evaluateTransaction(
    //                        4000, 12, false,
    //                        "RO", "FOOD",
    //                        1, 5000,
    //                        Set.of("IR", "RU"),
    //                        Set.of()
    //                );
    //
    //        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    //    }
}
