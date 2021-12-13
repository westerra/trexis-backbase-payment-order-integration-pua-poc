package net.trexis.experts.payments.exception;

import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import lombok.EqualsAndHashCode;
import com.backbase.buildingblocks.presentation.errors.Error;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class PaymentOrdersServiceException extends InternalServerErrorException {
    private List<Error> errors = new ArrayList<>();

    public List<Error> getErrors() {
        return this.errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public PaymentOrdersServiceException withErrors(List<Error> errors) {
        this.errors = errors;
        return this;
    }
}