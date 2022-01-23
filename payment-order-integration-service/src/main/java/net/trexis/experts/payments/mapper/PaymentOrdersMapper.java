package net.trexis.experts.payments.mapper;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.finite.api.commons.Utilities.DateUtilities;
import io.swagger.codegen.v3.service.exception.BadRequestException;
import net.trexis.experts.payments.configuration.PaymentConfiguration;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.apache.commons.lang3.StringUtils;
import com.finite.api.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

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
            schedule.setStartDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getSchedule().getStartDate().toString()));

            //The end date calculation takes place in Finite, as it will be specific per core/customer
            if(paymentConfiguration.getSchedule().getDefaultEndDate()!=null){
                schedule.setEndDateTime(paymentConfiguration.getSchedule().getDefaultEndDate());
            }

            exchangeTransaction.setRecurringSchedule(schedule);
        } else if(!paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now())){
            var schedule = new Schedule();
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            schedule.setFrequency("ONCE");
            schedule.setStartDateTime(exchangeTransaction.getExecutionDate());
            //Add 1 week as expiration for future transfers.
            schedule.setEndDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getRequestedExecutionDate().plusWeeks(1).toString()));
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


    private static String makeValidISODateTime(String date){
        try {
            Instant isoDateTimeInstant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(date));
            return DateUtilities.convertToISODateTime(new Date(isoDateTimeInstant.toEpochMilli()));
        } catch (DateTimeParseException e) {
            try{
                Instant isoDateTimeInstant = Instant.from(DateTimeFormatter.ISO_DATE.parse(date));
                return DateUtilities.convertToISODateTime(new Date(isoDateTimeInstant.toEpochMilli()));
            } catch (DateTimeParseException e2) {
                throw new BadRequestException("Unable to parse date to ISO");
            }
        }
    }
}