package bashkirov.store_original.dto;

import bashkirov.store_original.model.CartItem;
import bashkirov.store_original.model.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCartItemsDto {
    private Orders order;
    private List<CartItem> cartItem;
}
