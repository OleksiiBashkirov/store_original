package bashkirov.store_original.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private int id;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 2, max = 64, message = "Size should be at least 2 and not longer 64 characters")
    private String title;

    @Min(0)
    @Max(100_000)
    private BigDecimal price;

    private String article;

    private int countLeft;

    @NotBlank(message = "Field cannot be empty")
    private String description;

    private int categoryId;
}
