package bashkirov.store_original.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSaleDto {
    private int productId;

    @Min(value = 1, message = "Sale price cannot be less 0")
    private double salePrice;

    @Min(value = 1, message = "Hours cannot be less 1")
    private int hours;
}
