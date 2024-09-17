package net.trexis.experts.payments.mapper;

import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.finite.api.commons.Utilities.DateUtilities;
import io.swagger.codegen.v3.service.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.apache.commons.lang3.StringUtils;
import com.finite.api.model.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class PaymentOrdersMapper {

    public static final String CACHE_EXTERNAL_ID = "id";

    public static ExchangeTransaction createPaymentsOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, FiniteConfiguration finiteConfiguration, String zoneId) {
        var exchangeTransaction = new ExchangeTransaction();
        exchangeTransaction.setIsRecurring(Boolean.FALSE);
        exchangeTransaction.setId(paymentOrdersPostRequestBody.getId());
        exchangeTransaction.setAmount(new BigDecimal(paymentOrdersPostRequestBody.getTransferTransactionInformation().getInstructedAmount().getAmount()));

        exchangeTransaction.setExecutionDate(makeValidISODateTime(paymentOrdersPostRequestBody.getRequestedExecutionDate().toString()));
        if(paymentOrdersPostRequestBody.getTransferTransactionInformation() != null &&
                paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation() != null &&
                !StringUtils.isEmpty(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent())) {
            exchangeTransaction.setDescription(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent());
        }

        //In Finite the debitor is the From
        var accountDebtor = new AccountDebtor();
        accountDebtor.setDebtorType("AccountDebtor");
        accountDebtor.setId(paymentOrdersPostRequestBody.getOriginatorAccount().getExternalArrangementId());

        //In Finite the creditor is the To
        var accountCreditor = new AccountCreditor();
        accountCreditor.setCreditorType("AccountCreditor");
        accountCreditor.setId(paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getExternalArrangementId());

        exchangeTransaction.setDebtor(accountDebtor);
        exchangeTransaction.setCreditor(accountCreditor);

        //Set Recurring Schedule

        if(!paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            var schedule = new Schedule();
            exchangeTransaction.setIsRecurring(Boolean.TRUE);
            schedule.setRepeatCount(paymentOrdersPostRequestBody.getSchedule().getRepeat());
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            String finitePaymentFrequency = finiteConfiguration.getFiniteFromBackbaseMapping(finiteConfiguration.getPaymentFrequencies(), paymentOrdersPostRequestBody.getSchedule().getTransferFrequency().toString());
            schedule.setFrequency(finitePaymentFrequency);
            schedule.setIsEveryTime(Boolean.TRUE);
            schedule.setDayOn(paymentOrdersPostRequestBody.getSchedule().getOn().toString());
            schedule.setStartDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getSchedule().getStartDate().toString()));

            //The end date calculation takes place in Finite, as it will be specific per core/customer
            if(paymentOrdersPostRequestBody.getSchedule().getEndDate()!=null){
                schedule.setEndDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getSchedule().getEndDate().toString()));
            }

            exchangeTransaction.setRecurringSchedule(schedule);
        } else if(!paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now(ZoneId.of(zoneId)))){
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

    public static PaymentOrderStatus createPaymentsOrderStatusFromRequest(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, String zoneId) {
        //Return processed if requested date is the same as today and payment type is single as core is handling this immediately
        if(paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            if(paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now(ZoneId.of(zoneId)))) {
                log.debug("Execution Date is immediate, saving payment order as PROCESSED");
                return PaymentOrderStatus.PROCESSED;
            }
        }

        log.debug("Execution Date is not immediate, saving payment order as ACCEPTED");
        return PaymentOrderStatus.ACCEPTED;
    }


    private static String makeValidISODateTime(String dateString){
        try {
            if(DateUtilities.validateISODateTime(dateString)){
                return dateString;
            } else {
                if(DateUtilities.validateISODateOnly(dateString)){
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    return DateUtilities.convertToISODateTime(formatter.parse(dateString));
                } else {
                    Instant isoDateTimeInstant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(dateString));
                    return DateUtilities.convertToISODateTime(new Date(isoDateTimeInstant.toEpochMilli()));
                }
            }
        } catch (Exception e) {
            try{
                Instant isoDateTimeInstant = Instant.from(DateTimeFormatter.ISO_DATE.parse(dateString));
                return DateUtilities.convertToISODateTime(new Date(isoDateTimeInstant.toEpochMilli()));
            } catch (Exception e2) {
                throw new BadRequestException("Unable to parse date to ISO: " + dateString);
            }
        }
    }

    public static ExchangeTransaction createPaymentsOrdersforNewAccount(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, Account account, FiniteConfiguration finiteConfiguration, String zoneId) {
        var exchangeTransaction = new ExchangeTransaction();
        exchangeTransaction.setIsRecurring(Boolean.FALSE);
        exchangeTransaction.setId(paymentOrdersPostRequestBody.getId());
        exchangeTransaction.setAmount(new BigDecimal(paymentOrdersPostRequestBody.getTransferTransactionInformation().getInstructedAmount().getAmount()));
        exchangeTransaction.setExecutionDate(makeValidISODateTime(paymentOrdersPostRequestBody.getRequestedExecutionDate().toString()));

        // set description for transaction
        /*
        if(paymentOrdersPostRequestBody.getTransferTransactionInformation() != null &&
                paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation() != null &&
                !StringUtils.isEmpty(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent())) {
            exchangeTransaction.setDescription(paymentOrdersPostRequestBody.getTransferTransactionInformation().getRemittanceInformation().getContent());
        }
     */

        //In Finite the debitor is the From
        var accountDebtor = new AccountDebtor();
        accountDebtor.setDebtorType("AccountDebtor");
        accountDebtor.setId(paymentOrdersPostRequestBody.getOriginatorAccount().getExternalArrangementId());

        //In Finite the creditor is the To
        var accountCreditor = new AccountCreditor();
        accountCreditor.setCreditorType("AccountCreditor");

        //TODO get this for account create response

        String  accountCode = account.getId();
        String  newAccountId = account.getProduct().getId();
        String arrangementNewAccount = accountCode+ "-" + "S" + "-" + newAccountId;
        accountCreditor.setId(arrangementNewAccount);
        exchangeTransaction.setDebtor(accountDebtor);
        exchangeTransaction.setCreditor(accountCreditor);

        //Set Recurring Schedule

        if (!paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            var schedule = new Schedule();
            exchangeTransaction.setIsRecurring(Boolean.TRUE);
            schedule.setRepeatCount(paymentOrdersPostRequestBody.getSchedule().getRepeat());
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            String finitePaymentFrequency = finiteConfiguration.getFiniteFromBackbaseMapping(finiteConfiguration.getPaymentFrequencies(), paymentOrdersPostRequestBody.getSchedule().getTransferFrequency().toString());
            schedule.setFrequency(finitePaymentFrequency);
            schedule.setIsEveryTime(Boolean.TRUE);
            schedule.setDayOn(paymentOrdersPostRequestBody.getSchedule().getOn().toString());
            schedule.setStartDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getSchedule().getStartDate().toString()));

            //The end date calculation takes place in Finite, as it will be specific per core/customer
            if (paymentOrdersPostRequestBody.getSchedule().getEndDate() != null) {
                schedule.setEndDateTime(makeValidISODateTime(paymentOrdersPostRequestBody.getSchedule().getEndDate().toString()));
            }

            exchangeTransaction.setRecurringSchedule(schedule);
        } else if (!paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now(ZoneId.of(zoneId)))) {
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
}