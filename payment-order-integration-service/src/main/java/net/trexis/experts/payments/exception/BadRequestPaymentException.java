package net.trexis.experts.payments.exception;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.Error;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class BadRequestPaymentException extends BadRequestException {
    private List<Error> errors = new ArrayList<>();

    public List<Error> getErrors() {
        return this.errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public BadRequestPaymentException withErrors(List<Error> errors) {
        this.errors = errors;
        return this;
    }

    public BadRequestPaymentException(String message) {
        super(message);
    }
}
