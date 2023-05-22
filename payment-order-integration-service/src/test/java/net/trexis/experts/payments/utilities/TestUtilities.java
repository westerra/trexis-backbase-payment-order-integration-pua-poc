package net.trexis.experts.payments.utilities;

import com.backbase.buildingblocks.jwt.internal.token.InternalJwt;
import com.backbase.buildingblocks.jwt.internal.token.InternalJwtClaimsSet;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrderPutRequestBody;
import com.backbase.dbs.payment.payment_order_integration_outbound.model.PaymentOrdersPostRequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.finite.api.model.ExchangeTransactionResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestUtilities {

    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
            );


    public ExchangeTransactionResult getExchangeTransactionResult(String status, String reasonMessage,
            String transactionId) {
        ExchangeTransactionResult exchangeTransactionResult = new ExchangeTransactionResult();
        exchangeTransactionResult.setExchangeTransactionId(transactionId);
        exchangeTransactionResult.setStatus(status);
        exchangeTransactionResult.setReason(reasonMessage);
        return exchangeTransactionResult;
    }

    public PaymentOrdersPostRequestBody getPaymentOrderPost(String fileName) throws IOException {
        try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("/" + fileName))) {
            return objectMapper.readValue(reader, PaymentOrdersPostRequestBody.class);
        } catch (IOException e) {
            throw e;
        }
    }

    public PaymentOrderPutRequestBody getPaymentOrderPut(String fileName) throws IOException {
        try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("/" + fileName))) {
            return objectMapper.readValue(reader, PaymentOrderPutRequestBody.class);
        } catch (IOException e) {
            throw e;
        }
    }

    public Optional<InternalJwt> getOptionalJWT(String externalUserId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", externalUserId);
        InternalJwtClaimsSet internalJwtClaimsSet = new InternalJwtClaimsSet(claims);
        InternalJwt internalJwt = new InternalJwt("fakeToken", internalJwtClaimsSet);
        return Optional.of(internalJwt);
    }
}
