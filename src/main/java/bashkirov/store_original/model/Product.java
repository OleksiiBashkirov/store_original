package bashkirov.store_original.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

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

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{12}$")
    private String article;

    private int countLeft;

    @NotBlank(message = "Field cannot be empty")
    private String description;

    @Column(value = "category_id")
    private Category category;
}
