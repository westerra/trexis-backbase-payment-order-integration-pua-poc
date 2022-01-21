package net.trexis.experts.payments.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.HashMap;
import java.util.Map;

public enum PaymentOrderStatus {
    PROCESSED("PROCESSED"),
    REJECTED("REJECTED"),
    ACCEPTED("ACCEPTED");

    private final String value;
    private final static Map<String, PaymentOrderStatus> CONSTANTS = new HashMap<>();

    static {
        for (PaymentOrderStatus status : values()) {
            CONSTANTS.put(status.getValue(), status);
        }
    }

    PaymentOrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    @JsonGetter
    public String toString() {
        return getValue();
    }

    @JsonCreator
    public static PaymentOrderStatus fromValue(String value) {
        PaymentOrderStatus status = CONSTANTS.get(value);
        if (null == status) {
            throw new IllegalArgumentException(value);
        }
        return status;
    }

}
