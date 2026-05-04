package transaction;

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
                detector.evaluateTransaction(-100, 11, false, "RO", "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void lowerValueHourInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, -10, false, "RO", "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void upperValueHourInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 26, false, "RO", "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void countryCodeNull_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, null, "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void countryCodeBlank_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, " ", "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void merchantCategoryNull_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", null
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void merchantCategoryBlank_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", ""
                        , 5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void transactionsLast24hInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", "GROCERY STORES"
                        , -3, 50.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void averagePreviousAmountInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", "GROCERY STORES"
                        , 5, -1000.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void highRiskCountriesInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", "GROCERY STORES"
                        , 5, 50.0, null, blacklistedMerchants));
    }

    @Test
    void blacklistedMerchantsInvalid_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(100, 11, false, "RO", "GROCERY STORES"
                        , 5, 50.0, highRiskCountries, null));
    }

    @Test
    void lowRiskTransaction_shouldBeApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(70, 11, false, "RO", "GROCERY STORES"
                        , 5, 0.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void mediumRiskTransaction_shouldRequire2FA() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(2500, 3, false, "RO", "DENTAL AND MEDICAL LABORATORIES"
                        , 15, 1000.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void highRiskTransaction_shouldRequireManualReview() {
        assertEquals(TransactionDecision.MANUAL_REVIEW,
                detector.evaluateTransaction(4000, 23, true, "KP", "SHOE STORES"
                        , 5, 2000.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void veryHighRiskTransaction_shouldBeBlockedByScore() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(6000, 12, true, "IR", "GROCERY STORES"
                        , 15, 1000.0, highRiskCountries, blacklistedMerchants));
    }

    @Test
    void veryHighRiskTransaction_shouldBeBlockedDirectly() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(500, 12, false, "RO", "GAMBLING"
                        , 5, 200.0, highRiskCountries, blacklistedMerchants));
    }
}
