package facultate.tss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFraudDetectorConditionCoverageTest {

    private final TransactionFraudDetector detector = new TransactionFraudDetector();

    private final Set<String> highRiskCountries = Set.of("KP", "IR", "MM");

    private final Set<String> blacklistedMerchants = Set.of("GAMBLING", "CRYPTO_EXCHANGE", "FIREARMS");

    // TS1-TS5: cele 5 blocuri "throw" reprezentabile in CSV (amount, hour, country, merchant, history).
    // Reluate din suita pentru acoperirea la nivel de instructiune; impreuna cu TC1-TC5 acopera 100%
    // dintre conditiile individuale din blocurile de validare.
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, country={4}, merchant={5}, tx24={6}, avg={7} -> " +
            "IllegalArgumentException")
    @CsvSource(value = {
            "TS1, -1.0,  12, false, RO,   GROCERY, 5,  50.0",
            "TS2, 100.0, 24, false, RO,   GROCERY, 5,  50.0",
            "TS3, 100.0, 12, false, null, GROCERY, 5,  50.0",
            "TS4, 100.0, 12, false, RO,   null,    5,  50.0",
            "TS5, 100.0, 12, false, RO,   GROCERY, -1, 50.0"
    }, nullValues = "null")
    void invalidInput_throwsIllegalArgumentException(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, String merchant, int tx24, double avgPrev) {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        amount, hour, newBeneficiary, country, merchant,
                        tx24, avgPrev, highRiskCountries, blacklistedMerchants));
    }

    // TS6: validarea seturilor null (highRiskCountries == null).
    @Test
    void TS6_riskSetsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, null, blacklistedMerchants));
    }

    // TS7-TS11: scurtcircuit blacklist + cele patru decizii finale.
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, newBen={3}, country={4}, merchant={5}, tx24={6}, avg={7} ->" +
            " {8}")
    @CsvSource({
            "TS7,  100.0,   12, false, RO, GAMBLING, 5,  50.0,    BLOCKED",
            "TS8,  10000.0, 23, true,  KP, GROCERY,  15, 100.0,   BLOCKED",
            "TS9,  100.0,   12, false, RO, GROCERY,  5,  50.0,    APPROVED",
            "TS10, 2500.0,  12, false, RO, GROCERY,  15, 2000.0,  REQUIRES_2FA",
            "TS11, 5500.0,  23, true,  RO, GROCERY,  5,  10000.0, MANUAL_REVIEW"
    })
    void validInput_returnsExpectedDecision(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, String merchant, int tx24, double avgPrev,
            TransactionDecision expected) {
        assertEquals(expected,
                detector.evaluateTransaction(
                        amount, hour, newBeneficiary, country, merchant,
                        tx24, avgPrev, highRiskCountries, blacklistedMerchants));
    }

    // TC1-TC4: cele 4 lacune de tip "throw" reprezentabile in CSV (operand drept = true in expresia ||).
    // TC1: hour < 0 = true (al doilea operand al D2 era acoperit de TS2 cu hour = 24).
    // TC2: countryCode.isBlank() = true (al doilea operand al D3; TS3 acoperea doar countryCode == null).
    // TC3: merchantCategory.isBlank() = true (al doilea operand al D4; TS4 acoperea doar merchant == null).
    // TC4: averagePreviousAmount < 0 = true cu tx24 >= 0 (al doilea operand al D5; TS5 acoperea tx24 < 0).
    @ParameterizedTest(name = "{0}: amount={1}, hour={2}, country={4}, merchant={5}, tx24={6}, avg={7} -> " +
            "IllegalArgumentException")
    @CsvSource(value = {
            "TC1, 100.0, -1, false, RO, GROCERY, 5, 50.0",
            "TC2, 100.0, 12, false, '  ', GROCERY, 5, 50.0",
            "TC3, 100.0, 12, false, RO, '', 5, 50.0",
            "TC4, 100.0, 12, false, RO, GROCERY, 5, -1.0"
    })
    void missingValidationConditions_throwIllegalArgumentException(
            String testId, double amount, int hour, boolean newBeneficiary,
            String country, String merchant, int tx24, double avgPrev) {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        amount, hour, newBeneficiary, country, merchant,
                        tx24, avgPrev, highRiskCountries, blacklistedMerchants));
    }

    // TC5: blacklistedMerchants == null = true cu highRiskCountries != null
    // (al doilea operand al D6; TS6 acoperea doar highRiskCountries == null).
    @Test
    void TC5_blacklistedMerchantsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                detector.evaluateTransaction(
                        100.0, 12, false, "RO", "GROCERY",
                        5, 50.0, highRiskCountries, null));
    }

    // TC6: consolideaza simultan cele 4 lacune de pe calea de scor (C6, C7, C8, C9):
    //   - D10:  hourOfDay < 6 = true (hour = 3) -> +15
    //   - D11:  newBeneficiary = true cu amount > 3000 = false (amount = 2000) -> nu adauga
    //   - D14:  isHighRiskCountry = true (country = "KP") cu amount > 3000 = false -> nu adauga
    //   - D16:  averagePreviousAmount > 0 = false (avg = 0) -> nu adauga
    // Scor final: 10 (amount > 1000) + 15 (hour < 6) = 25 -> APPROVED.
    @Test
    void TC6_scorePathConditions_returnsApproved() {
        TransactionDecision actual = detector.evaluateTransaction(
                2000.0, 3, true, "KP", "GROCERY",
                5, 0.0, highRiskCountries, blacklistedMerchants);
        assertEquals(TransactionDecision.APPROVED, actual);
    }
}
