package bashkirov.store_original.model;

import bashkirov.store_original.enumeration.OrdersStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Orders {
    private int id;
    private int personId;
    private OrdersStatus ordersStatus;

    @PastOrPresent
    private LocalDateTime createdAt;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 7, max = 128, message = "Address should be like `01234, Country, other address parts")
    @Pattern(regexp = "^\\d{5}, [a-zA-Z]{2,20}, [a-zA-Z0-9 ,.\\-']+$")
    private String deliveryAddress;

    @Size(max = 1000)
    private String comment;
}
