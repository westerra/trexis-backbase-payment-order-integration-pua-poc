package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.*;
import com.finite.api.model.ExchangeTransactionResult;

import java.time.ZoneId;
import java.util.Optional;
import net.trexis.experts.finite.FiniteConfiguration;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody.PaymentModeEnum;
import java.time.LocalDate;
import net.trexis.experts.ingestion_service.api.IngestionApi;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import net.trexis.experts.payments.exception.PaymentOrdersServiceException;
import net.trexis.experts.payments.mapper.PaymentOrdersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.finite.api.ExchangeApi;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrdersService {
    public static final String XTRACE = "xtrace";
    public static final String INTRABANK_TRANSFER = "INTRABANK_TRANSFER";
    private final ExchangeApi exchangeApi;
    private final IngestionApi ingestionApi;
    private final FiniteConfiguration finiteConfiguration;

    @Value("${rejectRecurringStartingToday.enabled:false}")
    private boolean rejectRecurringStartingTodayEnabled;
    @Value("${rejectRecurringStartingToday.message}")
    private String rejectRecurringStartingTodayMessage;
    @Value("${timeZone.zoneId:America/Denver}")
    private String zoneId;

    @Value("${transferToContact.externalArrangementIdFormat:%s-S-00}")
    private String externalArrangementIdFormat;
    @Value("${transferToContact.leftPadAccountNumber:true}")
    private boolean leftPadAccountNumber;
    @Value("${transferToContact.leftPadLength:10}")
    private int leftPadLength;
    @Value("${transferToContact.leftPadChar:0}")
    private String leftPadChar;


    public PaymentOrdersPostResponseBody postPaymentOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, String externalUserId) {
        log.debug("BB Payment Request {}", paymentOrdersPostRequestBody);

        if (rejectRecurringStartingTodayEnabled &&
                paymentOrdersPostRequestBody.getPaymentMode() == PaymentModeEnum.RECURRING &&
                LocalDate.now(ZoneId.of(zoneId)).isEqual(paymentOrdersPostRequestBody.getRequestedExecutionDate())) {
            return new PaymentOrdersPostResponseBody()
                    .bankStatus(PaymentOrderStatus.REJECTED.getValue())
                    .reasonText(rejectRecurringStartingTodayMessage);
        }

        if (INTRABANK_TRANSFER.equals(paymentOrdersPostRequestBody.getPaymentType())) {
            Optional.ofNullable(paymentOrdersPostRequestBody.getTransferTransactionInformation())
                    .map(TransferTransactionInformation::getCounterpartyAccount)
                    .map(CounterpartyAccount::getIdentification)
                    .filter(it -> it.getSchemeName() == SchemeName.BBAN)
                    .map(Identification::getIdentification)
                    .map(accountNumber -> leftPadAccountNumber
                            ? StringUtils.leftPad(accountNumber, leftPadLength, leftPadChar)
                            : accountNumber)
                    .map(accountNumber -> String.format(externalArrangementIdFormat, accountNumber))
                    .ifPresent(externalArrangementId -> paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().externalArrangementId(externalArrangementId));
        }

        try {
            var exchangeTransaction = PaymentOrdersMapper.createPaymentsOrders(paymentOrdersPostRequestBody, finiteConfiguration);

            log.debug("Sending Payload to Finite Exchange {}", exchangeTransaction);

            var exchangeTransactionResult =
                    exchangeApi.performExchangeTransaction(exchangeTransaction, null, null);
            log.debug("Payment with result {}", exchangeTransactionResult.toString());
            if (exchangeTransactionResult == null || StringUtils.isEmpty(exchangeTransactionResult.getExchangeTransactionId())) {
                throw new PaymentOrdersServiceException().withMessage(getBBCompatibleReason(exchangeTransactionResult));
            }

            var paymentOrderStatus =
                    PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody);
            //Send refresh request on exchange.
            if (paymentOrderStatus.equals(PaymentOrderStatus.PROCESSED)) {
                this.triggerIngestion(externalUserId);
            }
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankReferenceId(exchangeTransactionResult.getExchangeTransactionId());
            paymentOrdersPostResponseBody.setBankStatus(paymentOrderStatus.getValue());
            // This field has a max of 4 characters
            Optional.ofNullable(exchangeTransactionResult.getStatus())
                    .map(this::maxLength4)
                    .ifPresent(paymentOrdersPostResponseBody::reasonCode);
            paymentOrdersPostResponseBody.setReasonText(exchangeTransactionResult.getReason());
            return paymentOrdersPostResponseBody;

        } catch (RuntimeException ex) {
            //Mark the payment order as rejected due to unknown submission error to core
            log.error("Error while exchanging transaction: {}", ex);
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankStatus(PaymentOrderStatus.REJECTED.getValue());
            paymentOrdersPostResponseBody.setReasonText(ex.getMessage());
            return paymentOrdersPostResponseBody;
        }
    }

    public PaymentOrderPutResponseBody updatePaymentOrder(String exchangeId, PaymentOrderPutRequestBody putRequestBody, String externalUserId) {
        try {
            log.debug("BB Payment Request {}", putRequestBody);
            var exchangeTransaction = PaymentOrdersMapper.createPaymentsOrders(putRequestBody, finiteConfiguration);
            log.debug("Sending Payload to Finite Exchange {}", exchangeTransaction);

            var exchangeTransactionResult =
                    exchangeApi.updateExchangeTransaction(exchangeId, exchangeTransaction, null, null);
            log.debug("Payment with result {}", exchangeTransactionResult.toString());
            if(exchangeTransactionResult == null || StringUtils.isEmpty(exchangeTransactionResult.getExchangeTransactionId())) {
                throw new PaymentOrdersServiceException().withMessage(getBBCompatibleReason(exchangeTransactionResult));
            }
            var paymentOrderStatus =
                    PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(putRequestBody);
            //Send refresh request on exchange.
            if(paymentOrderStatus.equals(PaymentOrderStatus.PROCESSED)) {
                this.triggerIngestion(externalUserId);
            }
            var paymentOrderPutResponseBody = new PaymentOrderPutResponseBody();
            paymentOrderPutResponseBody.setBankReferenceId(exchangeTransactionResult.getExchangeTransactionId());
            paymentOrderPutResponseBody.setBankStatus(paymentOrderStatus.getValue());
            // This field has a max of 4 characters
            Optional.ofNullable(exchangeTransactionResult.getStatus())
                    .map(this::maxLength4)
                    .ifPresent(paymentOrderPutResponseBody::reasonCode);
            paymentOrderPutResponseBody.setReasonText(exchangeTransactionResult.getReason());
            return paymentOrderPutResponseBody;

        } catch (RuntimeException ex) {
            //Mark the payment order as rejected due to unknown submission error to core
            log.error("Error while exchanging transaction: {}", ex);
            var paymentOrderPutResponseBody = new PaymentOrderPutResponseBody();
            paymentOrderPutResponseBody.setBankStatus(PaymentOrderStatus.REJECTED.getValue());
            paymentOrderPutResponseBody.setReasonText(ex.getMessage());
            return paymentOrderPutResponseBody;
        }
    }

    public CancelResponse cancelPaymentOrder(String bankReferenceId) {
        var isAccepted = Boolean.FALSE;
        try {
            log.debug("Cancelling transfer with exchange id {}", bankReferenceId);
            exchangeApi.deleteExchangeTransaction(bankReferenceId, null, null);
            isAccepted = Boolean.TRUE;
        } catch (RuntimeException ex) {
            log.error("Error while deleting payment order with bank reference id {} exception {}",bankReferenceId, ex);
        }
        var cancelResponse = new CancelResponse();
        cancelResponse.setAccepted(isAccepted);
        return cancelResponse;
    }

    private void triggerIngestion(String externalUserId){
        if(externalUserId!=null){
            try{
                //We ingest the entire user, so that balances on accounts get updated, including notifications get triggerded
                ingestionApi.getStartEntityIngestion(externalUserId, true);
            } catch (Exception ex){
                log.error("Error triggering ingestion", ex);
            }
        }
    }

    private String getBBCompatibleReason(ExchangeTransactionResult exchangeTransactionResult){
        String compatibleReason = "Unable to process payment order";
        if (exchangeTransactionResult.getReason() != null && !exchangeTransactionResult.getReason().isEmpty()) {
            compatibleReason = exchangeTransactionResult.getReason();
            //ToDo:  Logged a defect at backbase that the reason is limited to 32 characters, and our reasons are longer.
            if(compatibleReason.length()>35) {
                log.warn("Original error message truncated, value before truncate -> " + compatibleReason);
                compatibleReason = compatibleReason.substring(0, 32) + "..."; //Add ... to indicate it got truncated
            }
        }
        return compatibleReason;
    }

    private String maxLength4(String input) {
        if (input == null) {
            return "";
        }
        return input.length() > 4
                ? input.substring(0, 4)
                : input;
    }
}

