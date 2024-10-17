package bashkirov.store_original.dto;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductPhotoDto {
    private Product product;
    private ProductPhoto productPhoto;
}
