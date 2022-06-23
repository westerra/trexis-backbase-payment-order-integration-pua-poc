package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostResponseBody;
import com.finite.api.ExchangeApi;
import com.finite.api.model.ExchangeTransactionResult;
import com.google.gson.Gson;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.ingestion_service.api.IngestionApi;
import net.trexis.experts.payments.exception.PaymentOrdersServiceException;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOrdersServiceTest {

    private ExchangeApi exchangeApi = mock(ExchangeApi.class);
    private IngestionApi ingestionApi = mock(IngestionApi.class);
    private FiniteConfiguration finiteConfiguration = new FiniteConfiguration();

    @Test
    void postPaymentOrdersInternalTransferImmediateHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = getPaymentOrderPost("internal_transfer_immediate.yaml");

        ExchangeTransactionResult exchangeTransactionResult = getExchangeTransactionResult("true", "Well Done", "FakeId");

        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(paymentOrdersPostResponseBody.getReasonCode(), exchangeTransactionResult.getStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText(), exchangeTransactionResult.getReason());
        assertEquals(paymentOrdersPostResponseBody.getBankReferenceId(), exchangeTransactionResult.getExchangeTransactionId());
    }
    @Test
    void postPaymentOrdersInternalTransferImmediateUnHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = getPaymentOrderPost("internal_transfer_immediate.yaml");

        ExchangeTransactionResult exchangeTransactionResult = getExchangeTransactionResult("false", "Something went wrong with a long message in the response", null);

        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText().length(), 35);
    }

    private ExchangeTransactionResult getExchangeTransactionResult(String status, String reasonMessage, String transactionId){
        ExchangeTransactionResult exchangeTransactionResult = new ExchangeTransactionResult();
        exchangeTransactionResult.setExchangeTransactionId(transactionId);
        exchangeTransactionResult.setStatus(status);
        exchangeTransactionResult.setReason(reasonMessage);
        return  exchangeTransactionResult;
    }

    private PaymentOrdersPostRequestBody getPaymentOrderPost(String fileName) throws IOException {
        try (Reader reader = new InputStreamReader(this.getClass()
                .getResourceAsStream("/" + fileName))) {
            return new Gson().fromJson(reader, PaymentOrdersPostRequestBody.class);
        } catch (IOException e) {
            throw e;
        }
    }
}