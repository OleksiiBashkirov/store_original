package bashkirov.store_original.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPhoto {
    private int id;

    @NotBlank(message = "Field cannot be empty")
    @Pattern(regexp = "^(http|https)://\\S+", message = "URL should start with `http://` or `https://` and cannot contain spaces")
    private String url;

    private boolean isPrimary;
}
