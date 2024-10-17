package bashkirov.store_original.dto;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPhotosDto {
    private Product product;
    private ProductPhoto productPhoto;
    private List<ProductPhoto> productPhotoList;
}
