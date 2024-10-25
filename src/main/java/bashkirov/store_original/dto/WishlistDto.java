package bashkirov.store_original.dto;

import bashkirov.store_original.model.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WishlistDto {
    private Wishlist wishList;
    private ProductPhotoDto productPhotoDto;
}
