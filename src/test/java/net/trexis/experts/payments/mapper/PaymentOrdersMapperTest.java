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

        LocalDate weeklyLocalDate = PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody);
        Assert.assertEquals("2021-04-22", weeklyLocalDate.toString());
        //BiWeekly
        schedule.setTransferFrequency(Schedule.TransferFrequencyEnum.BIWEEKLY);
        paymentOrdersPostRequestBody.setSchedule(schedule);
        LocalDate biWeeklyLocalDate = PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody);
        Assert.assertEquals("2021-05-13", biWeeklyLocalDate.toString());
        //Monthly
        schedule.setTransferFrequency(Schedule.TransferFrequencyEnum.MONTHLY);
        paymentOrdersPostRequestBody.setSchedule(schedule);
        LocalDate monthlyWeeklyLocalDate = PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody);
        Assert.assertEquals("2021-07-01", monthlyWeeklyLocalDate.toString());
        //Quarterly
        schedule.setTransferFrequency(Schedule.TransferFrequencyEnum.QUARTERLY);
        paymentOrdersPostRequestBody.setSchedule(schedule);
        LocalDate quarterlyLocalDate = PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody);
        Assert.assertEquals("2022-01-01", quarterlyLocalDate.toString());
        //Yearly
        schedule.setTransferFrequency(Schedule.TransferFrequencyEnum.YEARLY);
        paymentOrdersPostRequestBody.setSchedule(schedule);
        LocalDate yearlyLocalDate = PaymentOrdersMapper.calculateEndDateTimeFromRepeat(paymentOrdersPostRequestBody);
        Assert.assertEquals("2024-04-01", yearlyLocalDate.toString());
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