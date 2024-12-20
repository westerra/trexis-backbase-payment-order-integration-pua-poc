package net.trexis.experts.payments.controller;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.payment.payment_order_integration_outbound.api.PaymentOrderIntegrationOutboundApi;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PurposeOfPayment;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.TransferTransactionInformation;
import lombok.RequiredArgsConstructor;
import net.trexis.experts.payments.service.PaymentOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentOrdersController implements PaymentOrderIntegrationOutboundApi {
    public static final String NEW_ACCOUNT_REQUEST = "NewAccount";
    private final PaymentOrdersService paymentOrdersService;
    private final SecurityContextUtil securityContextUtil;

    @Override
    public ResponseEntity<CancelResponse> postCancelPaymentOrder(String bankReferenceId) {
        return ResponseEntity.ok(paymentOrdersService.cancelPaymentOrder(bankReferenceId));
    }

    @Override
    public ResponseEntity<PaymentOrdersPostResponseBody> postPaymentOrders(
            PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {
        String externalUserId = null;
        // Attempt to fetch external user ID from JWT if present
        if (securityContextUtil.getOriginatingUserJwt().isPresent()) {
            externalUserId = securityContextUtil.getOriginatingUserJwt().get().getClaimsSet().getSubject().orElse(null);
        }

        // Extract account creation request data name and split to get account code and creation flag
        String newAccountRequestData = Optional.ofNullable(paymentOrdersPostRequestBody.getTransferTransactionInformation())
                .map(TransferTransactionInformation::getPurposeOfPayment)
                .map(PurposeOfPayment::getFreeText)
                .orElse(null);

        // Decision-making based on the createNewAccountFlag
        if (NEW_ACCOUNT_REQUEST.equalsIgnoreCase(newAccountRequestData)) {
            return ResponseEntity.ok(paymentOrdersService.createAccountAndPostPaymentOrders(paymentOrdersPostRequestBody));
        } else {
            return ResponseEntity.ok(paymentOrdersService.postPaymentOrders(paymentOrdersPostRequestBody, externalUserId));
        }
    }

    @Override
    public ResponseEntity<PaymentOrderPutResponseBody> putPaymentOrder(String bankReferenceId, PaymentOrderPutRequestBody paymentOrderPutRequestBody) {
        String externalUserId = null;
        if(securityContextUtil.getOriginatingUserJwt().isPresent()){
            externalUserId = securityContextUtil.getOriginatingUserJwt().get().getClaimsSet().getSubject().get();
        }

        // TODO: find out whether bankReferenceId is the same as Finite exchangeId. If not, we'll need to do some type of lookup
        return ResponseEntity.ok(paymentOrdersService.updatePaymentOrder(bankReferenceId, paymentOrderPutRequestBody, externalUserId));
    }
}