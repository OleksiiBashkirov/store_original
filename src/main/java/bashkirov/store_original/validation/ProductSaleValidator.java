package bashkirov.store_original.validation;

import bashkirov.store_original.dto.ProductSaleDto;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProductSaleValidator implements Validator {
    private final ProductService productService;

    @Override
    public boolean supports(Class<?> clazz) {
        return Objects.equals(clazz, ProductSaleValidator.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ProductSaleDto productSaleDto = (ProductSaleDto) target;
        Product product = productService.getById(productSaleDto.getProductId());
        if (productSaleDto.getSalePrice() >= product.getPrice()) {
            errors.rejectValue(
                    "sale_price",
                    "",
                    "Ціна має бути нижче за існуючу. Існуюча:" + product.getPrice() + ", нова:" + productSaleDto.getSalePrice()
            );
        }
    }
}
