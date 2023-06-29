package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CounterpartyAccount;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.Identification;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody.PaymentModeEnum;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.SchemeName;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.TransferTransactionInformation;
import com.finite.api.ExchangeApi;
import com.finite.api.model.ExchangeTransactionResult;

import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.ingestion_service.api.IngestionApi;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import net.trexis.experts.ingestion_service.model.StartIngestionPostRequest;
import net.trexis.experts.payments.exception.PaymentOrdersServiceException;
import net.trexis.experts.payments.mapper.PaymentOrdersMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrdersService {
    public static final String XTRACE = "xtrace";
    public static final String INTRABANK_TRANSFER = "INTRABANK_TRANSFER";
    public static final String ARRANGEMENT_ID_FORMATTER = "%010d";
    public static final String PRODUCT_ID_FORMATTER = "-S-0";
    private final ExchangeApi exchangeApi;
    private final IngestionApi ingestionApi;
    private final FiniteConfiguration finiteConfiguration;
    private final ArrangementsApi arrangementsApi;

    @Value("${rejectRecurringStartingToday.enabled:false}")
    private boolean rejectRecurringStartingTodayEnabled;
    @Value("${rejectRecurringStartingToday.message}")
    private String rejectRecurringStartingTodayMessage;

    @Value("${transferToContact.externalArrangementIdFormat:%s-S-00}")
    private String externalArrangementIdFormat;
    @Value("${transferToContact.leftPadAccountNumber:true}")
    private boolean leftPadAccountNumber;
    @Value("${transferToContact.leftPadLength:10}")
    private int leftPadLength;
    @Value("${transferToContact.leftPadChar:0}")
    private String leftPadChar;

    @Value("${timeZone.zoneId:America/Denver}")
    private String zoneId;

    Executor async = new Executor() {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    };

    public PaymentOrdersPostResponseBody postPaymentOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody, String externalUserId) {
        log.info("BB Payment Request {} ", paymentOrdersPostRequestBody);
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
            var exchangeTransaction = PaymentOrdersMapper.createPaymentsOrders(paymentOrdersPostRequestBody, finiteConfiguration, zoneId);

            log.info("Sending Payload to Finite Exchange {}", exchangeTransaction);

            var exchangeTransactionResult =
                    exchangeApi.performExchangeTransaction(exchangeTransaction, null, null);
            if(exchangeTransactionResult!=null) {
               log.info("Payment with result exchangeTransactionResult {}", exchangeTransactionResult.toString());
            }
            if (exchangeTransactionResult == null || StringUtils.isEmpty(exchangeTransactionResult.getExchangeTransactionId())) {
                throw new PaymentOrdersServiceException().withMessage(getBBCompatibleReason(exchangeTransactionResult.getReason()));
            }
            log.info("Create payment order status with {}", paymentOrdersPostRequestBody.toString());
            var paymentOrderStatus =
                    PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody, zoneId);
            //Send refresh request on exchange.

            log.info("Trigger ingestion with {}", paymentOrderStatus.toString());
            if (paymentOrderStatus.equals(PaymentOrderStatus.PROCESSED)) {
                var counterpartyAccountArrangementId = paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getArrangementId() != null ? paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getArrangementId() :
                        getArrangementIdFromExternalArrangementId(paymentOrdersPostRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getIdentification());
                this.triggerIngestion(externalUserId,
                        counterpartyAccountArrangementId != null ?
                                List.of(paymentOrdersPostRequestBody.getOriginatorAccount().getArrangementId(), counterpartyAccountArrangementId) :
                                List.of(paymentOrdersPostRequestBody.getOriginatorAccount().getArrangementId()));
            }
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankReferenceId(exchangeTransactionResult.getExchangeTransactionId());
            paymentOrdersPostResponseBody.setBankStatus(paymentOrderStatus.getValue());
            // This field has a max of 4 characters
            Optional.ofNullable(exchangeTransactionResult.getStatus())
                    .map(rawValue -> this.truncateTo(rawValue, 4))
                    .ifPresent(paymentOrdersPostResponseBody::reasonCode);
            // This field has a max of 35 characters
            Optional.ofNullable(exchangeTransactionResult.getReason())
                    .map(rawValue -> this.truncateTo(rawValue, 35))
                    .ifPresent(paymentOrdersPostResponseBody::setReasonText);

            return paymentOrdersPostResponseBody;

        } catch (RuntimeException ex) {
            //Mark the payment order as rejected due to unknown submission error to core
            log.error("Error while exchanging transaction: {}", ex);
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankStatus(PaymentOrderStatus.REJECTED.getValue());
            paymentOrdersPostResponseBody.setErrorDescription(getBBCompatibleErrorDescription(ex.getMessage()));
            paymentOrdersPostResponseBody.setReasonText(getBBCompatibleReason(ex.getMessage()));
            return paymentOrdersPostResponseBody;
        }
    }

    private String getArrangementIdFromExternalArrangementId(Identification identification) {
        // make account number to external arrangement id. 120521 -> 0000120521-S-0 , 1003 -> 0000001003-S-0
        try {
            String externalArrangementId = String.format(ARRANGEMENT_ID_FORMATTER,Integer.parseInt(identification.getIdentification())).concat(PRODUCT_ID_FORMATTER);
            List<AccountArrangementItem> arrangementElements = arrangementsApi.getArrangements(null, new ArrayList<>(), new ArrayList<>(Arrays.asList(externalArrangementId))).getArrangementElements();
            return arrangementElements==null || arrangementElements.size()==0 ? null : arrangementElements.get(0).getId();
        } catch (RuntimeException ex) {
            log.error("Exception while getting arrangement details for identification: {} : {}", identification, ex);
            return null;
        }
    }

    public PaymentOrderPutResponseBody updatePaymentOrder(String exchangeId, PaymentOrderPutRequestBody putRequestBody, String externalUserId) {
        try {
            log.debug("BB Payment Request {}", putRequestBody);
            var exchangeTransaction = PaymentOrdersMapper.createPaymentsOrders(putRequestBody, finiteConfiguration, zoneId);
            log.debug("Sending Payload to Finite Exchange {}", exchangeTransaction);

            var exchangeTransactionResult =
                    exchangeApi.updateExchangeTransaction(exchangeId, exchangeTransaction, null, null);
            log.debug("Payment with result {}", exchangeTransactionResult.toString());
            if(exchangeTransactionResult == null || StringUtils.isEmpty(exchangeTransactionResult.getExchangeTransactionId())) {
                throw new PaymentOrdersServiceException().withMessage(getBBCompatibleReason(exchangeTransactionResult.getReason()));
            }
            var paymentOrderStatus =
                    PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(putRequestBody, zoneId);
            //Send refresh request on exchange.
            if(paymentOrderStatus.equals(PaymentOrderStatus.PROCESSED)) {
              this.triggerIngestion(externalUserId, List.of(putRequestBody.getOriginatorAccount().getArrangementId(), putRequestBody.getTransferTransactionInformation().getCounterpartyAccount().getArrangementId()));
            }
            var paymentOrderPutResponseBody = new PaymentOrderPutResponseBody();
            paymentOrderPutResponseBody.setBankReferenceId(exchangeTransactionResult.getExchangeTransactionId());
            paymentOrderPutResponseBody.setBankStatus(paymentOrderStatus.getValue());
            // This field has a max of 4 characters
            Optional.ofNullable(exchangeTransactionResult.getStatus())
                    .map(rawValue -> this.truncateTo(rawValue, 4))
                    .ifPresent(paymentOrderPutResponseBody::reasonCode);
            paymentOrderPutResponseBody.setReasonText(getBBCompatibleReason(exchangeTransactionResult.getReason()));
            return paymentOrderPutResponseBody;

        } catch (RuntimeException ex) {
            //Mark the payment order as rejected due to unknown submission error to core
            log.error("Error while exchanging transaction: {}", ex);
            var paymentOrderPutResponseBody = new PaymentOrderPutResponseBody();
            paymentOrderPutResponseBody.setBankStatus(PaymentOrderStatus.REJECTED.getValue());
            paymentOrderPutResponseBody.setErrorDescription(getBBCompatibleErrorDescription(ex.getMessage()));
            paymentOrderPutResponseBody.setReasonText(getBBCompatibleReason(ex.getMessage()));
            return paymentOrderPutResponseBody;
        }
    }

    public CancelResponse cancelPaymentOrder(String bankReferenceId) {
        var isAccepted = Boolean.FALSE;
        try {
            log.debug("Cancelling transfer with exchange id {}", bankReferenceId);
            ExchangeTransactionResult exchangeTransactionResult = exchangeApi.deleteExchangeTransaction(bankReferenceId, null, null);
            if(exchangeTransactionResult.getStatus()!=null && Boolean.valueOf(exchangeTransactionResult.getStatus())){
                isAccepted = Boolean.TRUE;
            } else {
                log.error("Error deleting payment order {}: {}", bankReferenceId, exchangeTransactionResult.getReason());
            }
        } catch (RuntimeException ex) {
            log.error("Error while deleting payment order with bank reference id {} exception {}",bankReferenceId, ex);
        }
        var cancelResponse = new CancelResponse();
        cancelResponse.setAccepted(isAccepted);
        return cancelResponse;
    }

    private void triggerIngestion(String externalUserId, List<String> internalArrangementIds){
        if(externalUserId!=null){
            async.execute(() -> {
                try {
                    //We ingest the entire user, so that balances on accounts get updated, including notifications get triggerded
                    ingestionApi.startPostEntityIngestion(externalUserId, new StartIngestionPostRequest().internalArrangementIds(internalArrangementIds));
                } catch (Exception ex) {
                    log.error("Error triggering ingestion", ex);
                }
            });
        }
    }

    private String getBBCompatibleReason(String reasonText){
        String compatibleReason = "Unable to process payment order";
        if (!StringUtils.isEmpty(reasonText)) {
            compatibleReason = reasonText;
            //ToDo:  Logged a defect at backbase that the reason is limited to 32 characters, and our reasons are longer.
            if(compatibleReason.length()>35) {
                log.warn("Original error message truncated, value before truncate -> " + compatibleReason);
                compatibleReason = compatibleReason.substring(0, 32) + "..."; //Add ... to indicate it got truncated
            }
        }
        return compatibleReason;
    }

    private String getBBCompatibleErrorDescription(String descriptionText){
        String compatibleReason = "Unable to process payment order";
        if (!StringUtils.isEmpty(descriptionText)) {
            compatibleReason = descriptionText;
            //ToDo:  Logged a defect at backbase that the reason is limited to 32 characters, and our reasons are longer.
            if(compatibleReason.length()>105) {
                log.warn("Original error message truncated, value before truncate -> " + compatibleReason);
                compatibleReason = compatibleReason.substring(0, 102) + "..."; //Add ... to indicate it got truncated
            }
        }
        return compatibleReason;
    }

    private String truncateTo(String input, Integer maxLength) {
        if (input == null) {
            return "";
        }
        return input.length() > maxLength
                ? input.substring(0, maxLength)
                : input;
    }
}

