package net.trexis.experts.payments.mapper;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import net.trexis.experts.payments.configuration.FiniteTransferFrequency;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.finite.api.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
public class PaymentOrdersMapper {
    private PaymentOrdersMapper() { }
    public static final String CACHE_EXTERNAL_ID = "id";

    public static ExchangeTransaction createPaymentsOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        var exchangeTransaction = new ExchangeTransaction();
        exchangeTransaction.setIsRecurring(Boolean.FALSE);
        exchangeTransaction.setId(paymentOrdersPostRequestBody.getId());
        exchangeTransaction.setAmount(new BigDecimal(paymentOrdersPostRequestBody.getTransferTransactionInformation().getInstructedAmount().getAmount()));
        exchangeTransaction.setExecutionDate(paymentOrdersPostRequestBody.getRequestedExecutionDate().toString());
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
            schedule.setFrequency(FiniteTransferFrequency.valueOf(paymentOrdersPostRequestBody.getSchedule().getTransferFrequency().toString()).getFrequency());
            schedule.setIsEveryTime(Boolean.TRUE);
            schedule.setDayOn(paymentOrdersPostRequestBody.getSchedule().getOn().toString());
            schedule.setStartDateTime(paymentOrdersPostRequestBody.getSchedule().getStartDate().toString());
            if(paymentOrdersPostRequestBody.getSchedule().getRepeat() != null) {
                schedule.setEndDateTime(PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody).toString());
                log.debug("Schedule End Date calculated as Start Date {}: Repeat {}: End Date", schedule.getStartDateTime(), paymentOrdersPostRequestBody.getSchedule().getRepeat(), schedule.getEndDateTime());
            }
            else if(paymentOrdersPostRequestBody.getSchedule().getEndDate() != null) {
                schedule.setEndDateTime(paymentOrdersPostRequestBody.getSchedule().getEndDate().toString());
            } else {
                schedule.setEndDateTime("2078-12-21");
            }
            exchangeTransaction.setRecurringSchedule(schedule);
        } else if(!paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now())){
            var schedule = new Schedule();
            schedule.setStrategy(Schedule.StrategyEnum.NONE);
            schedule.setFrequency("ONCE");
            schedule.setStartDateTime(exchangeTransaction.getExecutionDate());
            //Add 1 week as expiration for future transfers.
            schedule.setEndDateTime(paymentOrdersPostRequestBody.getRequestedExecutionDate().plusWeeks(1).toString());
            exchangeTransaction.setRecurringSchedule(schedule);
        }
        return exchangeTransaction;
    }

    public static LocalDate calculateEndDateTimeFromRepeat(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        switch(paymentOrdersPostRequestBody.getSchedule().getTransferFrequency()) {
            case WEEKLY:
                return paymentOrdersPostRequestBody.getSchedule().getStartDate().plusWeeks(paymentOrdersPostRequestBody.getSchedule().getRepeat());
            case BIWEEKLY:
                return paymentOrdersPostRequestBody.getSchedule().getStartDate().plusWeeks( 2 * paymentOrdersPostRequestBody.getSchedule().getRepeat());
            case MONTHLY:
                return paymentOrdersPostRequestBody.getSchedule().getStartDate().plusMonths(paymentOrdersPostRequestBody.getSchedule().getRepeat());
            case QUARTERLY:
                return paymentOrdersPostRequestBody.getSchedule().getStartDate().plusMonths( 3 * paymentOrdersPostRequestBody.getSchedule().getRepeat());
            case YEARLY:
                return paymentOrdersPostRequestBody.getSchedule().getStartDate().plusYears(paymentOrdersPostRequestBody.getSchedule().getRepeat());
            default:
                throw new IllegalStateException("Unexpected value: " + paymentOrdersPostRequestBody.getSchedule().getTransferFrequency());
        }
    }

    public static String createPaymentsOrderStatusFromRequest(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        //Return processed if requested date is the same as today and payment type is single as core is handling this immediately
        if(paymentOrdersPostRequestBody.getPaymentMode().equals(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE)) {
            if(paymentOrdersPostRequestBody.getRequestedExecutionDate().isEqual(LocalDate.now())) {
                log.debug("Execution Date is immediate, saving payment order as PROCESSED");
                return "PROCESSED";
            }
        }

        log.debug("Execution Date is not immediate, saving payment order as ACCEPTED");
        return "ACCEPTED";
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