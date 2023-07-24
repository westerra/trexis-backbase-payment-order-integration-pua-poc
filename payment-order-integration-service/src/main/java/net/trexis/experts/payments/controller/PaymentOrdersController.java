package net.trexis.experts.payments.controller;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.payment.payment_order_integration_outbound.api.PaymentOrderIntegrationOutboundApi;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.trexis.experts.payments.service.PaymentOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentOrdersController implements PaymentOrderIntegrationOutboundApi {
    private final PaymentOrdersService paymentOrdersService;
    private final SecurityContextUtil securityContextUtil;


    @Override
    public ResponseEntity<CancelResponse> postCancelPaymentOrder(String bankReferenceId) {
        return ResponseEntity.ok(paymentOrdersService.cancelPaymentOrder(bankReferenceId));
    }

    @Override
    public ResponseEntity<PaymentOrdersPostResponseBody> postPaymentOrders(
            PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        log.error("$$$$$$$$$$$$$$$$$$$$$$$ PaymentOrdersController.postPaymentOrders start $$$$$$$$$$$$$$$$$$$$$$$$$$$");
        justForDebugging(paymentOrdersPostRequestBody, null);
        String externalUserId = null;
        if(securityContextUtil.getOriginatingUserJwt().isPresent()){
            externalUserId = securityContextUtil.getOriginatingUserJwt().get().getClaimsSet().getSubject().get();
        }

        return ResponseEntity.ok(paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, externalUserId));
    }

    @Override
    public ResponseEntity<PaymentOrderPutResponseBody> putPaymentOrder(String bankReferenceId, PaymentOrderPutRequestBody paymentOrderPutRequestBody) {
        log.error("$$$$$$$$$$$$$$$$$$$$$$$ PaymentOrdersController.putPaymentOrder start $$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.error("$$$$$$ bankRefID: " + bankReferenceId);
        justForDebugging(null, paymentOrderPutRequestBody);
        String externalUserId = null;
        if(securityContextUtil.getOriginatingUserJwt().isPresent()){
            externalUserId = securityContextUtil.getOriginatingUserJwt().get().getClaimsSet().getSubject().get();
        }

        // TODO: find out whether bankReferenceId is the same as Finite exchangeId. If not, we'll need to do some type of lookup
        return ResponseEntity.ok(paymentOrdersService.updatePaymentOrder(bankReferenceId, paymentOrderPutRequestBody, externalUserId));
    }

    private static String justForDebugging(PaymentOrdersPostRequestBody request, PaymentOrderPutRequestBody paymentOrderPutRequestBody) {
        ObjectMapper om = new ObjectMapper();
        String json = null;
        try {
            if (null != request) json = om.writeValueAsString(request);
            else json = om.writeValueAsString(paymentOrderPutRequestBody);
            log.error("Request body::::::::");
            log.error(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}