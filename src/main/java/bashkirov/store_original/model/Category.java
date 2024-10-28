package bashkirov.store_original.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category {
    private int id;

    @NotBlank
    @Size(min = 2, max = 32, message = "Size should be at least 2 and not longer 32 characters")
    private String name;
}
