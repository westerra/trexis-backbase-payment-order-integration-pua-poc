package net.trexis.experts.payments.configuration;

import com.backbase.buildingblocks.presentation.errors.NotFoundException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfiguration {

    private List<PaymentFrequency> frequencies;

    @Data
    public static class PaymentFrequency {
        private String backbase;
        private String finite;
    }

    public String getBackbaseFrequency(String finiteFrequency){
        Optional<PaymentFrequency> optionalPaymentFrequency = frequencies.stream().filter(paymentFrequency -> paymentFrequency.getFinite().equalsIgnoreCase(finiteFrequency)).findFirst();
        if(optionalPaymentFrequency.isPresent()){
            return optionalPaymentFrequency.get().getBackbase();
        } else {
            throw new NotFoundException("Finite payment frequency not known");
        }
    }

    public String getFiniteFrequency(String backbaseFrequency){
        Optional<PaymentFrequency> optionalPaymentFrequency = frequencies.stream().filter(paymentFrequency -> paymentFrequency.getBackbase().equalsIgnoreCase(backbaseFrequency)).findFirst();
        if(optionalPaymentFrequency.isPresent()){
            return optionalPaymentFrequency.get().getFinite();
        } else {
            throw new NotFoundException("Backbase payment frequency not known");
        }
    }
}
