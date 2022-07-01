package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.CancelResponse;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody.PaymentModeEnum;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostResponseBody;
import com.finite.api.ExchangeApi;
import com.finite.api.model.ExchangeTransactionResult;
import java.time.LocalDate;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.ingestion_service.api.IngestionApi;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import net.trexis.experts.payments.utilities.TestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOrdersServiceTest {

    private ExchangeApi exchangeApi = mock(ExchangeApi.class);
    private IngestionApi ingestionApi = mock(IngestionApi.class);
    private FiniteConfiguration finiteConfiguration = new FiniteConfiguration();

    private TestUtilities testUtilities = new TestUtilities();

    @BeforeAll
    public static void setErrorLogging() {
        LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel(Logger.ROOT_LOGGER_NAME, LogLevel.ERROR);
    }

    @Test
    void postPaymentOrdersInternalTransferImmediateHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_immediate.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("true", "Well Done", "FakeId");

        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(paymentOrdersPostResponseBody.getReasonCode(), exchangeTransactionResult.getStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText(), exchangeTransactionResult.getReason());
        assertEquals(paymentOrdersPostResponseBody.getBankReferenceId(), exchangeTransactionResult.getExchangeTransactionId());
    }
    @Test
    void postPaymentOrdersInternalTransferImmediateUnHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_immediate.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("false", "Something went wrong with a long message in the response", null);
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText().length(), 35);
    }
    @Test
    void postPaymentOrdersInternalTransferScheduledHappyPath() throws IOException {
        List<FiniteConfiguration.BackbaseFiniteMapping> paymentFrequencies = new ArrayList<>();
        FiniteConfiguration.BackbaseFiniteMapping weeklyFrequency = new FiniteConfiguration.BackbaseFiniteMapping();
        weeklyFrequency.setBackbase("WEEKLY");
        weeklyFrequency.setFinite("WEEKLY");
        paymentFrequencies.add(weeklyFrequency);
        finiteConfiguration.setPaymentFrequencies(paymentFrequencies);

        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_schedule_weekly.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("true", "Well Done", "FakeId");

        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(paymentOrdersPostResponseBody.getReasonCode(), exchangeTransactionResult.getStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText(), exchangeTransactionResult.getReason());
        assertEquals(paymentOrdersPostResponseBody.getBankReferenceId(), exchangeTransactionResult.getExchangeTransactionId());
    }

    @Test
    void postPaymentOrdersInternalTransfer_RecurringToday_Rejected() throws IOException {
        List<FiniteConfiguration.BackbaseFiniteMapping> paymentFrequencies = new ArrayList<>();
        FiniteConfiguration.BackbaseFiniteMapping weeklyFrequency = new FiniteConfiguration.BackbaseFiniteMapping();
        weeklyFrequency.setBackbase("WEEKLY");
        weeklyFrequency.setFinite("WEEKLY");
        paymentFrequencies.add(weeklyFrequency);
        finiteConfiguration.setPaymentFrequencies(paymentFrequencies);

        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);

        String rejectionMessage = "The start date cannot be today's date. Please choose a future date.";

        ReflectionTestUtils.setField(paymentOrdersService, "rejectRecurringStartingTodayEnabled", true);
        ReflectionTestUtils.setField(paymentOrdersService, "rejectRecurringStartingTodayMessage", rejectionMessage);

        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_schedule_weekly.json");
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now());
        paymentOrdersPostRequestBody.setPaymentMode(PaymentModeEnum.RECURRING);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(rejectionMessage, paymentOrdersPostResponseBody.getReasonText(), rejectionMessage);
    }

    @Test
    void updatePaymentOrderHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrderPutRequestBody paymentOrderPutRequestBody = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("true", "Well Done", "FakeId");
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");
        assertEquals(paymentOrdersPostResponseBody.getReasonCode(), exchangeTransactionResult.getStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText(), exchangeTransactionResult.getReason());
        assertEquals(paymentOrdersPostResponseBody.getBankReferenceId(), exchangeTransactionResult.getExchangeTransactionId());
    }
    @Test
    void updatePaymentOrderUnHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);
        PaymentOrderPutRequestBody paymentOrderPutRequestBody = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("false", "Something went wrong with a long message in the response", null);
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");
        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText().length(), 35);
    }

    @Test
    void cancelPaymentOrderHappyPath() {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration);

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("true", "Well Done", "FakeId");
        when(exchangeApi.deleteExchangeTransaction(anyString(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        CancelResponse cancelResponse = paymentOrdersService.cancelPaymentOrder("fakeId");
        assertEquals(cancelResponse.getAccepted(), true);
    }

}