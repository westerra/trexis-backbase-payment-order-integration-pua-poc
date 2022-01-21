package net.trexis.experts.payments.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.HashMap;
import java.util.Map;

public enum FiniteTransferFrequency {
    WEEKLY("WEEKLY"),
    BIWEEKLY("BIWEEKLY"),
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    YEARLY("ANNUALLY");

    private final String value;
    private final static Map<String, FiniteTransferFrequency> CONSTANTS = new HashMap<>();

    static {
        for (FiniteTransferFrequency frequency : values()) {
            CONSTANTS.put(frequency.getFrequency(), frequency);
        }
    }

    FiniteTransferFrequency(String value) {
        this.value = value;
    }

    public String getFrequency() {
        return value;
    }

    @Override
    @JsonGetter
    public String toString() {
        return getFrequency();
    }

    @JsonCreator
    public static FiniteTransferFrequency fromValue(String value) {
        FiniteTransferFrequency frequency = CONSTANTS.get(value);
        if (null == frequency) {
            throw new IllegalArgumentException();
        }
        return frequency;
    }
}