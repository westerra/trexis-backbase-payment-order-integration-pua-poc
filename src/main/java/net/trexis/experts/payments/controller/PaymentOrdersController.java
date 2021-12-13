package net.trexis.experts.payments.controller;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.CancelResponse;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostResponseBody;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.PaymentOrderIntegrationOutboundApi;
import net.trexis.experts.payments.service.PaymentOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentOrdersController implements PaymentOrderIntegrationOutboundApi {
    private final PaymentOrdersService paymentOrdersService;

    @Autowired
    public PaymentOrdersController(PaymentOrdersService paymentOrdersService) {
        this.paymentOrdersService = paymentOrdersService;
    }

    @Override
    public ResponseEntity<CancelResponse> postCancelPaymentOrder(String bankReferenceId) {
        return ResponseEntity.ok(paymentOrdersService.cancelPaymentOrder(bankReferenceId));
    }

    @Override
    public ResponseEntity<PaymentOrdersPostResponseBody> postPaymentOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        return ResponseEntity.ok(paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody));
    }
}