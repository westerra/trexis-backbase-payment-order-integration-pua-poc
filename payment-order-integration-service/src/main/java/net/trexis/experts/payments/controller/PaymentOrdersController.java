package net.trexis.experts.payments.controller;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.*;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.PaymentOrderIntegrationOutboundApi;
import lombok.RequiredArgsConstructor;
import net.trexis.experts.payments.service.PaymentOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Size;

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
    public ResponseEntity<PaymentOrdersPostResponseBody> postPaymentOrders(PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        String externalUserId = null;
        if(securityContextUtil.getOriginatingUserJwt().isPresent()){
            externalUserId = securityContextUtil.getOriginatingUserJwt().get().getClaimsSet().getSubject().get();
        }

        return ResponseEntity.ok(paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, externalUserId));
    }

    @Override
    public ResponseEntity<PaymentOrderPutResponseBody> putPaymentOrder(@Size(max = 64) String bankReferenceId, @Valid PaymentOrderPutRequestBody paymentOrderPutRequestBody) {
        return null;
    }
}