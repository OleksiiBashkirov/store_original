package bashkirov.store_original.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoCode {
    private int id;

    @NotBlank(message = "Promo code cannot be blank")
    @Size(min = 4, max = 16, message = "Promo code should be between 4 and 16 characters")
    private String code;

    @Min(0)
    private double discount;
    private LocalDateTime expirationDate;
    private boolean isPercentage;
    private Integer categoryId;
}
