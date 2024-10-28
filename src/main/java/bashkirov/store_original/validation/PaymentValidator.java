package bashkirov.store_original.validation;

import bashkirov.store_original.dto.PaymentDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class PaymentValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Objects.equals(clazz, PaymentDto.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PaymentDto paymentDto = (PaymentDto) target;
        LocalDate now = LocalDate.now();

        if (paymentDto.getYear() < 2024 || paymentDto.getYear() > 2040 || paymentDto.getMonth() < 1 || paymentDto.getMonth() > 12) {
            errors.rejectValue("year", "", "Invalid input date");
            return;
        }

        LocalDate cardExpiredDate = LocalDate.of(paymentDto.getYear(), paymentDto.getMonth(), 28);
        if (cardExpiredDate.isBefore(now)) {
            errors.rejectValue(
                    "year",
                    "",
                    "Bank cart have been expired"
            );
        }
    }
}
