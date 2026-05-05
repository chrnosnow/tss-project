package facultate.tss;

import java.util.Set;

public class TransactionFraudDetector {

    public TransactionDecision evaluateTransaction(
            double amount,
            int hourOfDay,
            boolean newBeneficiary,
            String countryCode,
            String merchantCategory,
            int transactionsLast24h,
            double averagePreviousAmount,
            Set<String> highRiskCountries,
            Set<String> blacklistedMerchants
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (hourOfDay < 0 || hourOfDay > 23) {
            throw new IllegalArgumentException("Hour of day must be between 0 and 23");
        }

        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }

        if (merchantCategory == null || merchantCategory.isBlank()) {
            throw new IllegalArgumentException("Merchant category is required");
        }

        if (transactionsLast24h < 0 || averagePreviousAmount < 0) {
            throw new IllegalArgumentException("Transaction history values cannot be negative");
        }

        if (highRiskCountries == null || blacklistedMerchants == null) {
            throw new IllegalArgumentException("Risk sets cannot be null");
        }

        if (blacklistedMerchants.contains(merchantCategory)) {
            return TransactionDecision.BLOCKED;
        }

        int riskScore = 0;

        if (amount > 1000) {
            riskScore += 10;
        }

        if (amount > 5000) {
            riskScore += 20;
        }

        if (hourOfDay < 6 || hourOfDay > 22) {
            riskScore += 15;
        }

        if (newBeneficiary && amount > 3000) {
            riskScore += 25;
        }

        boolean isHighRiskCountry = false;
        for (String country : highRiskCountries) {
            if (country.equals(countryCode)) {
                isHighRiskCountry = true;
                break;
            }
        }
        if (isHighRiskCountry && amount > 3000) {
            riskScore += 30;
        }

        if (transactionsLast24h > 10) {
            riskScore += 20;
        }

        if (averagePreviousAmount > 0 && amount > averagePreviousAmount * 3) {
            riskScore += 25;
        }

        if (riskScore >= 90) {
            return TransactionDecision.BLOCKED;
        } else if (riskScore >= 60) {
            return TransactionDecision.MANUAL_REVIEW;
        } else if (riskScore >= 30) {
            return TransactionDecision.REQUIRES_2FA;
        } else {
            return TransactionDecision.APPROVED;
        }
    }
}
