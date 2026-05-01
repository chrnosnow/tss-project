import facultate.tss.TransactionDecision;
import facultate.tss.TransactionFraudDetector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFraudDetectorEquivalenceTest {
    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    @Test
    void amountInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(-100, 9, false, "RO", "GROCERY STORES"
                        , 5, 50, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void hourInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, -10, false, "RO", "GROCERY STORES"
                        , 5, 50, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void upperValueHourInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 26, false, "RO", "GROCERY STORES"
                        , 5, 50, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void lowRiskTransaction_shouldBeApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(70, 11, false, "RO", "GROCERY STORES"
                        , 5, 0, highRiskCountries, blacklistedMerchants));
    }
}
