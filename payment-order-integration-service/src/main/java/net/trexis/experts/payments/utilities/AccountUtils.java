package net.trexis.experts.payments.utilities;

import com.finite.api.model.Account;

public class AccountUtils {

    public static String generateArrangementNewAccount(Account account, String originatorExtArrangementId) {
        if (account == null || account.getProduct() == null || account.getProduct().getId() == null) {
            throw new IllegalArgumentException("Invalid account or product details");
        }

        String memberId = extractMemberId(originatorExtArrangementId);
        String newAccountId = account.getProduct().getId();
        return memberId + "-S-" + newAccountId;
    }

    private static String extractMemberId(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        String[] parts = data.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Data does not match the expected pattern");
        }

        return parts[0];
    }
}