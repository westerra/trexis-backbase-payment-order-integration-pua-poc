package net.trexis.experts.payments.mapper;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.Schedule;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentOrdersMapperTest {
    private final String zoneId = "America/Denver";

    @Test
    public void calculateEndDateTimeFromRepeat_Success() {
        java.time.LocalDate startDate = LocalDate.parse("2021-04-01");

        //Weekly
        var schedule = new Schedule();
        schedule.setTransferFrequency(Schedule.TransferFrequencyEnum.WEEKLY);
        schedule.setStartDate(startDate);
        schedule.setRepeat(3);
        var paymentOrdersPostRequestBody = new PaymentOrdersPostRequestBody();
        paymentOrdersPostRequestBody.setSchedule(schedule);
    }

    @Test
    public void singlePaymentOrderForTodayShouldGetStatusProcessed() {
        var paymentOrdersPostRequestBody = new PaymentOrdersPostRequestBody();
        paymentOrdersPostRequestBody.setPaymentMode(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE);
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now(ZoneId.of(zoneId)));

        assertEquals(PaymentOrderStatus.PROCESSED, PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody,zoneId));
    }

    @Test
    public void singlePaymentOrderForNextWeekShouldGetStatusAccepted() {
        var paymentOrdersPostRequestBody = new PaymentOrdersPostRequestBody();
        paymentOrdersPostRequestBody.setPaymentMode(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE);
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now(ZoneId.of(zoneId)).plusWeeks(1));

        assertEquals(PaymentOrderStatus.ACCEPTED, PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody,zoneId));
    }

    @Test
    public void paymentOrderStatusEnumShouldCreateEnumFromString() {
        var status = PaymentOrderStatus.fromValue("PROCESSED");
        assertEquals(PaymentOrderStatus.PROCESSED, status);
    }

    @Test
    public void paymentOrderStatusEnumShouldThrowOnInvalidFromValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            var badEnum = PaymentOrderStatus.fromValue("FAKE");
        });
    }
}