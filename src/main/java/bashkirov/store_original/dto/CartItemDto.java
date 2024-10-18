package bashkirov.store_original.dto;

import bashkirov.store_original.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {
    private CartItem cartItem;
    private ProductPhotoDto productPhotoDto;
}
