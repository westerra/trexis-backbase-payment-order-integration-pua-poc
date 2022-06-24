package net.trexis.experts.payments.utilities;

import com.backbase.buildingblocks.jwt.internal.token.InternalJwt;
import com.backbase.buildingblocks.jwt.internal.token.InternalJwtClaimsSet;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.finite.api.model.ExchangeTransactionResult;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestUtilities {

    public ExchangeTransactionResult getExchangeTransactionResult(String status, String reasonMessage, String transactionId){
        ExchangeTransactionResult exchangeTransactionResult = new ExchangeTransactionResult();
        exchangeTransactionResult.setExchangeTransactionId(transactionId);
        exchangeTransactionResult.setStatus(status);
        exchangeTransactionResult.setReason(reasonMessage);
        return  exchangeTransactionResult;
    }

    public PaymentOrdersPostRequestBody getPaymentOrderPost(String fileName) throws IOException {
        try (Reader reader = new InputStreamReader(this.getClass()
                .getResourceAsStream("/" + fileName))) {
            return new Gson().fromJson(reader, PaymentOrdersPostRequestBody.class);
        } catch (IOException e) {
            throw e;
        }
    }
    public PaymentOrderPutRequestBody getPaymentOrderPut(String fileName) throws IOException {
        try (Reader reader = new InputStreamReader(this.getClass()
                .getResourceAsStream("/" + fileName))) {
            return new Gson().fromJson(reader, PaymentOrderPutRequestBody.class);
        } catch (IOException e) {
            throw e;
        }
    }

    public Optional<InternalJwt> getOptionalJWT(String externalUserId){
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", externalUserId);
        InternalJwtClaimsSet internalJwtClaimsSet = new InternalJwtClaimsSet(claims);
        InternalJwt internalJwt = new InternalJwt("fakeToken", internalJwtClaimsSet);
        return Optional.of(internalJwt);
    }
}
