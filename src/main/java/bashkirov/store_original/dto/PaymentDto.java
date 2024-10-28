package bashkirov.store_original.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    @Pattern(regexp = "\\d{16}", message = "Bank cart should have 16 numbers")
    private String bankCart;

    private int year;

    private int month;

    @Pattern(regexp = "\\d{3}", message = "CVV number should have 3 numbers")
    private String cvvCode;
}
