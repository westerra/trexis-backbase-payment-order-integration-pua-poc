package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.CancelResponse;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostResponseBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import net.trexis.experts.payments.exception.PaymentOrdersServiceException;
import net.trexis.experts.payments.mapper.PaymentOrdersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.finite.api.CacheApi;
import com.finite.api.ExchangeApi;
import com.finite.api.model.AccountCreditor;
import com.finite.api.model.AccountDebtor;
import com.finite.api.model.ExchangeTransaction;
import com.finite.api.model.FiniteType;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrdersService {
    public static final String XTRACE = "xtrace";
    @Qualifier("symexchangeExchangeApi")
    private final ExchangeApi exchangeApi;

    @Qualifier("symexchangeCacheApi")
    private final CacheApi cacheApi;

    public PaymentOrdersPostResponseBody postPaymentOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        try {
            log.debug("BB Payment Request {}", paymentOrdersPostRequestBody);
            var exchangeTransaction = PaymentOrdersMapper.createPaymentsOrders(paymentOrdersPostRequestBody);
            log.debug("Sending Payload to Finite Exchange {}", exchangeTransaction);

            var exchangeTransactionResult =
                    exchangeApi.performExchangeTransaction(exchangeTransaction, null, null);
            log.debug("Payment with result {}", exchangeTransactionResult.toString());
            if(exchangeTransactionResult == null || StringUtils.isEmpty(exchangeTransactionResult.getExchangeTransactionId())) {
                throw new PaymentOrdersServiceException().withMessage("Unable to retrieve exchange transaction id from result");
            }
            var paymentOrderStatus =
                    PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody);
            //Send refresh request on exchange.
            if(paymentOrderStatus.equals("PROCESSED")) {
                this.renewFiniteAdminCache(exchangeTransaction);
            }
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankReferenceId(exchangeTransactionResult.getExchangeTransactionId());
            paymentOrdersPostResponseBody.setBankStatus(paymentOrderStatus);
            return paymentOrdersPostResponseBody;
        } catch (RuntimeException ex) {
            //Mark the payment order as rejected due to submission error to core
            log.error("Error while exchanging transaction: {}", ex);
            var paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
            paymentOrdersPostResponseBody.setBankStatus("REJECTED");
            return paymentOrdersPostResponseBody;
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

    private void renewFiniteAdminCache(ExchangeTransaction exchangeTransaction) {
        new Thread(() -> {
            log.debug("Starting Finite admin Cache Refresh ");
            var accountDebtor = (AccountDebtor)exchangeTransaction.getDebtor();
            var accountCreditor = (AccountCreditor)exchangeTransaction.getCreditor();
            //TODO: Refactor once batch ingestion is in place
            this.cacheApi.renewCache(FiniteType.ACCOUNT, PaymentOrdersMapper.toFiniteRefreshCacheReference(accountDebtor.getId()), XTRACE);
            this.cacheApi.renewCache(FiniteType.TRANSACTION, PaymentOrdersMapper.toFiniteRefreshCacheReference(accountDebtor.getId()), XTRACE);
            this.cacheApi.renewCache(FiniteType.ACCOUNT, PaymentOrdersMapper.toFiniteRefreshCacheReference(accountCreditor.getId()), XTRACE);
            this.cacheApi.renewCache(FiniteType.TRANSACTION, PaymentOrdersMapper.toFiniteRefreshCacheReference(accountCreditor.getId()), XTRACE);
        }).start();
    }
}

