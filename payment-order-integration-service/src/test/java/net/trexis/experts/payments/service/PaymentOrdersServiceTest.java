package net.trexis.experts.payments.service;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody.PaymentModeEnum;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import com.finite.api.ExchangeApi;
import com.finite.api.model.AccountCreditor;
import com.finite.api.model.ExchangeTransaction;
import com.finite.api.model.ExchangeTransactionResult;
import java.time.LocalDate;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.ingestion_service.api.IngestionApi;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import net.trexis.experts.payments.utilities.TestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOrdersServiceTest {

    private ExchangeApi exchangeApi = mock(ExchangeApi.class);
    private IngestionApi ingestionApi = mock(IngestionApi.class);
    private ArrangementsApi arrangementsApi = mock(ArrangementsApi.class);

    private FiniteConfiguration finiteConfiguration = new FiniteConfiguration();

    private TestUtilities testUtilities = new TestUtilities();
    private final String zoneId = "America/Denver";

    @BeforeAll
    public static void setErrorLogging() {
        LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel(Logger.ROOT_LOGGER_NAME, LogLevel.ERROR);
    }

    @Test
    void postPaymentOrdersInternalTransferImmediateHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
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
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
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

        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
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

        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        String rejectionMessage = "The start date cannot be today's date. Please choose a future date.";

        ReflectionTestUtils.setField(paymentOrdersService, "rejectRecurringStartingTodayEnabled", true);
        ReflectionTestUtils.setField(paymentOrdersService, "rejectRecurringStartingTodayMessage", rejectionMessage);

        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_schedule_weekly.json");
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now(ZoneId.of(zoneId)));
        paymentOrdersPostRequestBody.setPaymentMode(PaymentModeEnum.RECURRING);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");
        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(rejectionMessage, paymentOrdersPostResponseBody.getReasonText(), rejectionMessage);
    }

    @Test
    void postPaymentOrdersInternalTransfer_truncatesReasonCodeAbove4Characters() throws IOException {
        // common setup
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_immediate.json");

        // TEST CASE: 7 character reasonCode - should truncate to 4
        var reasonCode = "success";
        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult(reasonCode, "Well Done", "FakeId");
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        assertEquals(4, paymentOrdersPostResponseBody.getReasonCode().length());
        assertEquals(reasonCode.substring(0, 4), paymentOrdersPostResponseBody.getReasonCode());

        // TEST CASE: 4 character reasonCode - should not truncate
        reasonCode = "pass";
        exchangeTransactionResult = testUtilities.getExchangeTransactionResult(reasonCode, "Well Done", "FakeId");
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        assertEquals(reasonCode, paymentOrdersPostResponseBody.getReasonCode());
    }

    @Test
    void postPaymentOrdersInternalTransfer_truncatesReasonTextAbove35Characters() throws IOException {
        // common setup
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("internal_transfer_immediate.json");

        // TEST CASE: 50 character reasonText - should truncate to 35
        var reasonText = "H1UYjF99f82RoRnlKVHDCDZH6jjJ2dqEtg1Ak89c3HZCUDOLre";
        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("abcd", reasonText, "FakeId");
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        assertEquals(35, paymentOrdersPostResponseBody.getReasonText().length());
        assertEquals(reasonText.substring(0, 35), paymentOrdersPostResponseBody.getReasonText());

        // TEST CASE: 10 character reasonText - should not truncate
        reasonText = "k1lI7Symx1";
        exchangeTransactionResult = testUtilities.getExchangeTransactionResult("abcd", reasonText, "FakeId");
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        paymentOrdersPostResponseBody = paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        assertEquals(reasonText, paymentOrdersPostResponseBody.getReasonText());
    }

    @Test
    void postPaymentOrders_IntrabankTransfer_ShouldMapToAccountNumber() throws IOException {
        // setup
        List<FiniteConfiguration.BackbaseFiniteMapping> paymentFrequencies = new ArrayList<>();
        FiniteConfiguration.BackbaseFiniteMapping weeklyFrequency = new FiniteConfiguration.BackbaseFiniteMapping();
        weeklyFrequency.setBackbase("WEEKLY");
        weeklyFrequency.setFinite("WEEKLY");
        paymentFrequencies.add(weeklyFrequency);
        finiteConfiguration.setPaymentFrequencies(paymentFrequencies);

        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrdersPostRequestBody paymentOrdersPostRequestBody = testUtilities.getPaymentOrderPost("intrabank_transfer_recurring.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("pass", "Well Done", "FakeId");
        when(exchangeApi.performExchangeTransaction(any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        ReflectionTestUtils.setField(paymentOrdersService, "externalArrangementIdFormat", "%s-S-00");
        ReflectionTestUtils.setField(paymentOrdersService, "leftPadAccountNumber", true);
        ReflectionTestUtils.setField(paymentOrdersService, "leftPadLength", 10);
        ReflectionTestUtils.setField(paymentOrdersService, "leftPadChar", "0");

        // test
        paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, "mockExternalUserId");

        // verify the call to finite had a body with the correct external arrangement id
        ArgumentCaptor<ExchangeTransaction> exchangeTransactionCaptor = ArgumentCaptor.forClass(ExchangeTransaction.class);
        verify(exchangeApi, times(1)).performExchangeTransaction(exchangeTransactionCaptor.capture(), any(), any());
        assertEquals("0000917916-S-00", ((AccountCreditor) exchangeTransactionCaptor.getValue().getCreditor()).getId());
    }



    @Test
    void updatePaymentOrderHappyPath() throws IOException {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
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
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrderPutRequestBody paymentOrderPutRequestBody = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");

        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("false", "Something went wrong with a long message in the response", null);
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");
        assertEquals(PaymentOrderStatus.REJECTED.getValue(), paymentOrdersPostResponseBody.getBankStatus());
        assertEquals(paymentOrdersPostResponseBody.getReasonText().length(), 35);
    }

    @Test
    void updatePaymentOrder_truncatesReasonCodeAbove4Characters() throws IOException {
        // common setup
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrderPutRequestBody paymentOrderPutRequestBody = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");

        // TEST CASE: 7 character reasonCode - should truncate to 4
        var reasonCode = "success";
        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult(reasonCode, "Well Done", "FakeId");
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrderPutResponseBody paymentOrderPutResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");

        assertEquals(4, paymentOrderPutResponseBody.getReasonCode().length());
        assertEquals(reasonCode.substring(0, 4), paymentOrderPutResponseBody.getReasonCode());

        // TEST CASE: 4 character reasonCode - should not truncate
        reasonCode = "pass";
        exchangeTransactionResult = testUtilities.getExchangeTransactionResult(reasonCode, "Well Done", "FakeId");
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        paymentOrderPutResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");

        assertEquals(paymentOrderPutResponseBody.getReasonCode(), exchangeTransactionResult.getStatus());
    }

    @Test
    void updatePaymentOrder_truncatesReasonTextAbove35Characters() throws IOException {
        // common setup
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        PaymentOrderPutRequestBody paymentOrderPutRequestBody = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");

        // TEST CASE: 50 character reasonText - should truncate to 35
        var reasonText = "H1UYjF99f82RoRnlKVHDCDZH6jjJ2dqEtg1Ak89c3HZCUDOLre";
        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("abcd", reasonText, "FakeId");
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        PaymentOrderPutResponseBody paymentOrderPutResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");

        assertEquals(35, paymentOrderPutResponseBody.getReasonText().length());
        assertEquals("H1UYjF99f82RoRnlKVHDCDZH6jjJ2dqE...", paymentOrderPutResponseBody.getReasonText());

        // TEST CASE: 10 character reasonText - should not truncate
        reasonText = "k1lI7Symx1";
        exchangeTransactionResult = testUtilities.getExchangeTransactionResult(reasonText, "Well Done", "FakeId");
        when(exchangeApi.updateExchangeTransaction(anyString(), any(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        paymentOrderPutResponseBody = paymentOrdersService.updatePaymentOrder("FakeId", paymentOrderPutRequestBody, "mockExternalUserId");

        assertEquals(paymentOrderPutResponseBody.getReasonText(), exchangeTransactionResult.getReason());
    }


    @Test
    void cancelPaymentOrderHappyPath() {
        PaymentOrdersService paymentOrdersService = new PaymentOrdersService(exchangeApi, ingestionApi, finiteConfiguration, arrangementsApi);
        ReflectionTestUtils.setField(paymentOrdersService, "zoneId", zoneId);
        ExchangeTransactionResult exchangeTransactionResult = testUtilities.getExchangeTransactionResult("true", "Well Done", "FakeId");
        when(exchangeApi.deleteExchangeTransaction(anyString(), isNull(), isNull())).thenReturn(exchangeTransactionResult);

        CancelResponse cancelResponse = paymentOrdersService.cancelPaymentOrder("fakeId");
        assertEquals(cancelResponse.getAccepted(), true);
    }

}