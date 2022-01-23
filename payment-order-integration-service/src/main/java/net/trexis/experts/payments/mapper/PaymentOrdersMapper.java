package net.trexis.experts.payments.mapper;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.finite.api.commons.Utilities.DateUtilities;
import net.trexis.experts.payments.configuration.PaymentConfiguration;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.apache.commons.lang3.StringUtils;
import com.finite.api.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
public class PaymentOrdersMapper {

    public static final String CACHE_EXTERNAL_ID = "id";

    public static ExchangeTransaction createPaymentsOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, PaymentConfiguration paymentConfiguration) {
        var exchangeTransaction = new ExchangeTransaction();
        exchangeTransaction.setIsRecurring(Boolean.FALSE);
        exchangeTransaction.setId(paymentOrdersPostRequestBody.getId());
        exchangeTransaction.setAmount(new BigDecimal(paymentOrdersPostRequestBody.getTransferTransactionInformation().getInstructedAmount().getAmount()));

        String effectiveDate = paymentOrdersPostRequestBody.getRequestedExecutionDate().toString();
        if(DateUtilities.validateISODateOnly(effectiveDate)) effectiveDate += "T00:00:00";
        exchangeTransaction.setExecutionDate(effectiveDate);
        if(paymentOrdersPostRequestBody.getTransferTransactionInformation() != null &&
                paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation() != null &&
                !StringUtils.isEmpty(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent())) {
            exchangeTransaction.setDescription(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent());
        }
        var accountDebtor = new AccountDebtor();
        accountDebtor.setId(paymentOrdersPostRequestBody.getOriginatorAccount().getExternalArrangementId());
        accountDebtor.setDebtorType("AccountDebtor");

        var accountCreditor = new AccountCreditor();
        accountCreditor.setCreditorType("AccountCreditor");
        accountCreditor.setId(paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getExternalArrangementId());

        exchangeTransaction.setDebtor(accountDebtor);
        exchangeTransaction.setCreditor(accountCreditor);

        //Set Recurring Schedule

        if(!paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            var schedule = new Schedule();
            exchangeTransaction.setIsRecurring(Boolean.TRUE);
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            schedule.setFrequency(paymentConfiguration.getFiniteFrequency(paymentOrdersPostRequestBody.getSchedule().getTransferFrequency().toString()));
            schedule.setIsEveryTime(Boolean.TRUE);
            schedule.setDayOn(paymentOrdersPostRequestBody.getSchedule().getOn().toString());
            String isoStartDate = paymentOrdersPostRequestBody.getSchedule().getStartDate().toString();
            if(DateUtilities.validateISODateOnly(isoStartDate)) isoStartDate += "T00:00:00";
            schedule.setStartDateTime(isoStartDate);

            //The end date calculation takes place in Finite, as it will be specific per core/customer
            if(paymentConfiguration.getSchedule().getDefaultEndDate()!=null){
                String isoEndDate = paymentConfiguration.getSchedule().getDefaultEndDate();
                if(DateUtilities.validateISODateOnly(isoEndDate)) isoEndDate += "T00:00:00";
                schedule.setEndDateTime(isoEndDate);
            }

            exchangeTransaction.setRecurringSchedule(schedule);
        } else if(!paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now())){
            var schedule = new Schedule();
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            schedule.setFrequency("ONCE");
            schedule.setStartDateTime(exchangeTransaction.getExecutionDate());
            //Add 1 week as expiration for future transfers.
            String isoEndDate = paymentOrdersPostRequestBody.getRequestedExecutionDate().plusWeeks(1).toString();
            if(DateUtilities.validateISODateOnly(isoEndDate)) isoEndDate += "T00:00:00";
            schedule.setEndDateTime(isoEndDate);
            exchangeTransaction.setRecurringSchedule(schedule);
        }
        return exchangeTransaction;
    }

    public static PaymentOrderStatus createPaymentsOrderStatusFromRequest(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        //Return processed if requested date is the same as today and payment type is single as core is handling this immediately
        if(paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            if(paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now())) {
                log.debug("Execution Date is immediate, saving payment order as PROCESSED");
                return PaymentOrderStatus.PROCESSED;
            }
        }

        log.debug("Execution Date is not immediate, saving payment order as ACCEPTED");
        return PaymentOrderStatus.ACCEPTED;
    }

    public static CacheReference toFiniteRefreshCacheReference(String attributeValue) {
        var cacheReference = new CacheReference();
        var attribute = new Attribute();
        attribute.setName(CACHE_EXTERNAL_ID);
        attribute.setValue(attributeValue);
        cacheReference.addAttributesItem(attribute);
        return cacheReference;
    }
}