package transaction;

import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Teste pentru analiza valorilor de frontiera a clasei {@link TransactionFraudDetector}.
 * <p>
 * Cele 29 de cazuri sunt specificate in {@code docs/anexa-a-teste-frontiera.md};
 * fiecare rand de mai jos respecta ID-ul testului (TF-V1, TF-S1, TF-D1, ...).
 */
class TransactionFraudDetectorBoundaryTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    private static final String DEFAULT_MERCHANT = "GROCERY STORES";

    private TransactionDecision evaluate(
            double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev) {
        return detector.evaluateTransaction(
                amount, hour, newBeneficiary, country,
                DEFAULT_MERCHANT, tx24, avgPrev,
                highRiskCountries, blacklistedMerchants);
    }

    // F-V: Frontiere de validare - intrari invalide -> IllegalArgumentException
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, tx24={5}, avgPrev={6} -> IllegalArgumentException")
    @CsvSource({
            "TF-V1, 0.0,   12, false, RO, 5,  10000.0",
            "TF-V3, 100.0, -1, false, RO, 5,  10000.0",
            "TF-V6, 100.0, 24, false, RO, 5,  10000.0",
            "TF-V7, 100.0, 12, false, RO, -1, 10000.0",
            "TF-V9, 100.0, 12, false, RO, 5,  -0.01"
    })
    void validationBoundary_invalidInput_throwsIllegalArgumentException(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev) {
        assertThrows(IllegalArgumentException.class, () ->
                evaluate(amount, hour, newBeneficiary, country, tx24, avgPrev));
    }

    // F-V: Frontiere de validare - intrari valide pe limita
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, tx24={5}, avgPrev={6} -> {7}")
    @CsvSource({
            "TF-V2,  0.01,  12, false, RO, 5, 10000.0, APPROVED",
            "TF-V4,  100.0, 0,  false, RO, 5, 10000.0, APPROVED",
            "TF-V5,  100.0, 23, false, RO, 5, 10000.0, APPROVED",
            "TF-V8,  100.0, 12, false, RO, 0, 10000.0, APPROVED",
            "TF-V10, 100.0, 12, false, RO, 5, 0.0,     APPROVED"
    })
    void validationBoundary_validInput_returnsExpectedDecision(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                evaluate(amount, hour, newBeneficiary, country, tx24, avgPrev));
    }

    // F-S: Frontiere ale regulilor de scor
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, newBen={3}, tx24={5}, avgPrev={6} -> {7}")
    @CsvSource({
            "TF-S1,  1000.0,  12, false, RO, 5,  10000.0, APPROVED",
            "TF-S2,  1000.01, 12, false, RO, 5,  10000.0, APPROVED",
            "TF-S3,  5000.0,  12, false, RO, 5,  10000.0, APPROVED",
            "TF-S4,  5000.01, 12, false, RO, 5,  10000.0, REQUIRES_2FA",
            "TF-S5,  3000.0,  12, true,  RO, 5,  10000.0, APPROVED",
            "TF-S6,  3000.01, 12, true,  RO, 5,  10000.0, REQUIRES_2FA",
            "TF-S7,  100.0,   5,  false, RO, 5,  10000.0, APPROVED",
            "TF-S8,  100.0,   6,  false, RO, 5,  10000.0, APPROVED",
            "TF-S9,  100.0,   22, false, RO, 5,  10000.0, APPROVED",
            "TF-S10, 100.0,   12, false, RO, 10, 10000.0, APPROVED",
            "TF-S11, 100.0,   12, false, RO, 11, 10000.0, APPROVED",
            "TF-S12, 1500.0,  12, false, RO, 5,  500.0,   APPROVED",
            "TF-S13, 1500.01, 12, false, RO, 5,  500.0,   REQUIRES_2FA"
    })
    void scoreRuleBoundary_returnsExpectedDecision(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                evaluate(amount, hour, newBeneficiary, country, tx24, avgPrev));
    }

    // F-D: Frontiere ale deciziilor (praguri 30/60/90)
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, newBen={3}, country={4}, tx24={5} -> {7}")
    @CsvSource({
            "TF-D1, 1500.0, 23, false, RO, 5,  10000.0, APPROVED",
            "TF-D2, 5500.0, 12, false, RO, 5,  10000.0, REQUIRES_2FA",
            "TF-D3, 5500.0, 12, true,  RO, 5,  10000.0, REQUIRES_2FA",
            "TF-D4, 5500.0, 12, false, KP, 5,  10000.0, MANUAL_REVIEW",
            "TF-D5, 4000.0, 12, true,  KP, 11, 10000.0, MANUAL_REVIEW",
            "TF-D6, 5500.0, 23, true,  RO, 11, 10000.0, BLOCKED"
    })
    void decisionBoundary_returnsExpectedDecision(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                evaluate(amount, hour, newBeneficiary, country, tx24, avgPrev));
    }
}
