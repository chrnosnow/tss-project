package ai;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AiCovStatemTest {
    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    // 1. Exception: amount <= 0
    @Test
    void shouldThrowExceptionForInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        0, 10, false, "RO", "FOOD",
                        1, 100, Set.of(), Set.of()
                )
        );
    }

    // 2. Exception: invalid hour
    @Test
    void shouldThrowExceptionForInvalidHour() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 24, false, "RO", "FOOD",
                        1, 100, Set.of(), Set.of()
                )
        );
    }

    // 3. Exception: null country
    @Test
    void shouldThrowExceptionForNullCountry() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100, 10, false, null, "FOOD",
                        1, 100, Set.of(), Set.of()
                )
        );
    }

    // 4. Blacklisted merchant → BLOCKED
    @Test
    void shouldBlockBlacklistedMerchant() {
        TransactionDecision result = detector.evaluateTransaction(
                100, 10, false, "RO", "GAMBLING",
                1, 100,
                Set.of(),
                Set.of("GAMBLING")
        );

        assertEquals(TransactionDecision.BLOCKED, result);
    }

    // 5. APPROVED (riskScore < 30)
    @Test
    void shouldApproveLowRiskTransaction() {
        TransactionDecision result = detector.evaluateTransaction(
                100, 12, false, "RO", "FOOD",
                1, 100,
                Set.of(),
                Set.of()
        );

        assertEquals(TransactionDecision.APPROVED, result);
    }

    // 6. REQUIRES_2FA (riskScore >= 30)
    @Test
    void shouldRequire2FA() {
        TransactionDecision result = detector.evaluateTransaction(
                1500, 23, false, "RO", "FOOD",
                1, 100,
                Set.of(),
                Set.of()
        );

        assertEquals(TransactionDecision.REQUIRES_2FA, result);
    }

    // 7. MANUAL_REVIEW (riskScore >= 60)
    //    @Test
    //    void shouldRequireManualReview() {
    //        TransactionDecision result = detector.evaluateTransaction(
    //                6000, 23, true, "RO", "FOOD",
    //                5, 100,
    //                Set.of(),
    //                Set.of()
    //        );
    //
    //        assertEquals(TransactionDecision.MANUAL_REVIEW, result);
    //    }

    // 8. BLOCKED (riskScore >= 90)
    @Test
    void shouldBlockHighRiskTransaction() {
        TransactionDecision result = detector.evaluateTransaction(
                7000, 2, true, "IR", "FOOD",
                20, 100,
                Set.of("IR"),
                Set.of()
        );

        assertEquals(TransactionDecision.BLOCKED, result);
    }
}
