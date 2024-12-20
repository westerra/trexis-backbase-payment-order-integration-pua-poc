package net.trexis.experts.payments.controller;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import net.trexis.experts.payments.service.PaymentOrdersService;
import net.trexis.experts.payments.utilities.TestUtilities;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOrdersControllerTest {

    private PaymentOrdersService paymentOrdersService = mock(PaymentOrdersService.class);
    private SecurityContextUtil securityContextUtil = mock(SecurityContextUtil.class);

    private TestUtilities testUtilities = new TestUtilities();

    @Test
    void postCancelPaymentOrder() throws IOException {
        when(securityContextUtil.getOriginatingUserJwt()).thenReturn(testUtilities.getOptionalJWT("fakeExternalUserId"));

        PaymentOrdersController paymentOrdersController = new PaymentOrdersController(paymentOrdersService, securityContextUtil);

        CancelResponse cancelResponse = new CancelResponse();
        cancelResponse.setAccepted(true);
        when(paymentOrdersService.cancelPaymentOrder(anyString())).thenReturn(cancelResponse);

        ResponseEntity<CancelResponse> responseEntity = paymentOrdersController.postCancelPaymentOrder("FakeId");
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void postPaymentOrders() throws IOException {
        when(securityContextUtil.getOriginatingUserJwt()).thenReturn(testUtilities.getOptionalJWT("fakeExternalUserId"));

        PaymentOrdersController paymentOrdersController = new PaymentOrdersController(paymentOrdersService, securityContextUtil);

        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = new PaymentOrdersPostResponseBody();
        paymentOrdersPostResponseBody.setBankStatus(PaymentOrderStatus.ACCEPTED.getValue());
        when(paymentOrdersService.postPaymentOrders(any(), anyString())).thenReturn(paymentOrdersPostResponseBody);

        PaymentOrdersPostRequestBody paymentOrderPost = testUtilities.getPaymentOrderPost("internal_transfer_immediate.json");
        ResponseEntity<PaymentOrdersPostResponseBody> responseEntity = paymentOrdersController.postPaymentOrders(paymentOrderPost);

        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void putPaymentOrder() throws IOException {
        when(securityContextUtil.getOriginatingUserJwt()).thenReturn(testUtilities.getOptionalJWT("fakeExternalUserId"));

        PaymentOrdersController paymentOrdersController = new PaymentOrdersController(paymentOrdersService, securityContextUtil);

        PaymentOrderPutResponseBody paymentOrderPutRequestBody = new PaymentOrderPutResponseBody();
        paymentOrderPutRequestBody.setBankStatus(PaymentOrderStatus.ACCEPTED.getValue());
        when(paymentOrdersService.updatePaymentOrder(anyString(), any(), anyString())).thenReturn(paymentOrderPutRequestBody);

        PaymentOrderPutRequestBody paymentOrderPut = testUtilities.getPaymentOrderPut("internal_transfer_immediate.json");
        ResponseEntity<PaymentOrderPutResponseBody> responseEntity = paymentOrdersController.putPaymentOrder("FakeId", paymentOrderPut);

        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);

    }
}