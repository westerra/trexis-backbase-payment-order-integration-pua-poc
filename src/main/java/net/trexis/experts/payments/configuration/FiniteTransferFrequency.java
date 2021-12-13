package net.trexis.experts.payments.configuration;

public enum FiniteTransferFrequency {
    WEEKLY("WEEKLY"),
    BIWEEKLY("BIWEEKLY"),
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    YEARLY("ANNUALLY");

    public final String frequency;

    FiniteTransferFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getFrequency() {
        return frequency;
    }
}