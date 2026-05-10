package facultate.tss;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFraudDetectorIndependentPathsTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    // P1: drum de baza, toate deciziile pe ramura "fals"; scor 0 -> APPROVED.
    @Test
    void P1_baseline_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P2: D1 = true (amount <= 0).
    @Test
    void P2_invalidAmount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        -1.0, 12, false, "RO", "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P3: D2 = true (hour > 23).
    @Test
    void P3_invalidHour_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 24, false, "RO", "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P4: D3 = true (country null/blank).
    @Test
    void P4_invalidCountry_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, null, "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P5: D4 = true (merchant null/blank).
    @Test
    void P5_invalidMerchant_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", null,
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P6: D5 = true (tx24 < 0 sau avg < 0).
    @Test
    void P6_invalidHistory_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        -1, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P7: D6 = true (highRisk == null sau blacklist == null).
    @Test
    void P7_nullRiskSets_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, null, blacklistedMerchants));
    }

    // P8: D7 = true (merchant blacklistat -> BLOCKED).
    @Test
    void P8_blacklistedMerchant_returnsBlocked() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GAMBLING",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P9: D8 = true (amount > 1000); avg = 600 ales astfel incat 3 * avg > amount,
    // pentru a NU activa simultan regula multiplului mediei.
    @Test
    void P9_amountAbove1000_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        1500.0, 12, false, "RO", "GROCERY",
                        5, 600.0, highRiskCountries, blacklistedMerchants));
    }

    // P10: D9 = true (amount > 5000); D8 obligatoriu true (5500 > 1000).
    // Scor minim posibil = 10 + 20 = 30 -> REQUIRES_2FA (P20 implicit).
    @Test
    void P10_amountAbove5000_returnsRequires2FA() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        5500.0, 12, false, "RO", "GROCERY",
                        5, 2000.0, highRiskCountries, blacklistedMerchants));
    }

    // P11: D10 = true (hour > 22); restul baseline.
    @Test
    void P11_nightHour_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 23, false, "RO", "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P12: D11 = true (newBen && amount > 3000); D8 obligatoriu true.
    @Test
    void P12_newBeneficiaryHighAmount_returnsRequires2FA() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        3500.0, 12, true, "RO", "GROCERY",
                        5, 1500.0, highRiskCountries, blacklistedMerchants));
    }

    // P13: D12 = false (bucla `for` nu itereaza, set highRisk gol).
    // Comportament observabil identic cu P1, drum prin CFG diferit.
    @Test
    void P13_emptyHighRiskCountries_skipsLoop_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, Set.of(), blacklistedMerchants));
    }

    // P14: D13 = true (match in for + break). amount = 100 pentru a NU activa D14.
    @Test
    void P14_highRiskCountryMatch_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 12, false, "KP", "GROCERY",
                        5, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P15: D14 = true (isHighRisk && amount > 3000); D8 si D13 obligatoriu true.
    @Test
    void P15_highRiskCountryAndHighAmount_returnsRequires2FA() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        3500.0, 12, false, "KP", "GROCERY",
                        5, 1500.0, highRiskCountries, blacklistedMerchants));
    }

    // P16: D15 = true (tx24 > 10); restul baseline.
    @Test
    void P16_manyTransactions_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        15, 50.0, highRiskCountries, blacklistedMerchants));
    }

    // P17: D16 = true (avg > 0 && amount > 3 * avg); restul baseline.
    @Test
    void P17_amountExceedsAverageMultiple_returnsApproved() {
        assertEquals(TransactionDecision.APPROVED,
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 10.0, highRiskCountries, blacklistedMerchants));
    }

    // P18: D17 = true (scor >= 90); toate cele 7 reguli active + match in for.
    // Scor: 10+20+15+25+30+20+25 = 145 -> BLOCKED.
    @Test
    void P18_extremeRiskScore_returnsBlocked() {
        assertEquals(TransactionDecision.BLOCKED,
                detector.evaluateTransaction(
                        10000.0, 2, true, "KP", "GROCERY",
                        15, 10.0, highRiskCountries, blacklistedMerchants));
    }

    // P19: D18 = true (60 <= scor < 90).
    // Scor: 10+20+15+25 = 70 -> MANUAL_REVIEW.
    @Test
    void P19_highRiskScore_returnsManualReview() {
        assertEquals(TransactionDecision.MANUAL_REVIEW,
                detector.evaluateTransaction(
                        5500.0, 23, true, "RO", "GROCERY",
                        5, 2000.0, highRiskCountries, blacklistedMerchants));
    }

    // P20: D19 = true (30 <= scor < 60).
    // Scor: 10+20 = 30 -> REQUIRES_2FA.
    @Test
    void P20_mediumRiskScore_returnsRequires2FA() {
        assertEquals(TransactionDecision.REQUIRES_2FA,
                detector.evaluateTransaction(
                        2500.0, 12, false, "RO", "GROCERY",
                        15, 1000.0, highRiskCountries, blacklistedMerchants));
    }
}
