package net.trexis.experts.payments.mapper;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.PaymentOrdersPostRequestBody;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.Schedule;
import net.trexis.experts.payments.models.PaymentOrderStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

@RunWith(MockitoJUnitRunner.class)
public class PaymentOrdersMapperTest {
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
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now());

        Assert.assertEquals(PaymentOrderStatus.PROCESSED, PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody));
    }

    @Test
    public void singlePaymentOrderForNextWeekShouldGetStatusAccepted() {
        var paymentOrdersPostRequestBody = new PaymentOrdersPostRequestBody();

        paymentOrdersPostRequestBody.setPaymentMode(PaymentOrdersPostRequestBody.PaymentModeEnum.SINGLE);
        paymentOrdersPostRequestBody.setRequestedExecutionDate(LocalDate.now().plusWeeks(1));

        Assert.assertEquals(PaymentOrderStatus.ACCEPTED, PaymentOrdersMapper.createPaymentsOrderStatusFromRequest(paymentOrdersPostRequestBody));
    }

    @Test
    public void paymentOrderStatusEnumShouldCreateEnumFromString() {
        var status = PaymentOrderStatus.fromValue("PROCESSED");
        Assert.assertEquals(PaymentOrderStatus.PROCESSED, status);
    }

    @Test(expected = IllegalArgumentException.class)
    public void paymentOrderStatusEnumShouldThrowOnInvalidFromValue() {
        var badEnum = PaymentOrderStatus.fromValue("FAKE");
    }
}