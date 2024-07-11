package net.trexis.experts.payments.controller;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.payment.payment_order_integration_outbound.api.PaymentOrderIntegrationOutboundApi;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.CancelResponse;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutResponseBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostResponseBody;
import lombok.RequiredArgsConstructor;
import net.trexis.experts.payments.service.PaymentOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentOrdersController implements PaymentOrderIntegrationOutboundApi {
    private final PaymentOrdersService paymentOrdersService;
    private final SecurityContextUtil securityContextUtil;

    private static final String WESTERRA_CREATE_NEW_ACCOUNT = "westerraCreateNewAccount";
    private static final String YES = "yes";

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

        // Extract counterparty name and split to get account code and creation flag
        String counterpartyName = paymentOrdersPostRequestBody.getTransferTransactionInformation()
                .getCounterparty().getName();

        // Check if the counterparty name contains "westerraCreateNewAccount"
        String[] accountCodeAndCreateFlag = null;
        if (counterpartyName.contains("westerraCreateNewAccount")) {
            // Split the counterparty name to get the account code and the new account creation flag
            accountCodeAndCreateFlag = counterpartyName.split("-");

            // Check if the split result contains both the account code and the creation flag
            if (accountCodeAndCreateFlag.length < 2) {
                // Return a bad request response if the creation flag is missing
                log.error("Invalid Request for new Account Creation");
            }
        }

        String newAccountCreateFlag = accountCodeAndCreateFlag != null ? accountCodeAndCreateFlag[1] : null;
        // Decision-making based on the createNewAccountFlag
        if ("westerraCreateNewAccount".equalsIgnoreCase(newAccountCreateFlag)) {
            return ResponseEntity.ok(paymentOrdersService.createAccountAndPostPaymentOrders(paymentOrdersPostRequestBody, externalUserId));
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