package bashkirov.store_original.validation;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductValidator implements Validator {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean supports(Class<?> clazz) {
        return Objects.equals(clazz, Product.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Product product = (Product) target;
        Optional<Product> optionalProduct = jdbcTemplate.query(
                "select * from product where article = ?",
                new Object[]{product.getArticle()},
                ProductService.getProductRowMapper()
        ).stream().findAny();

        if (optionalProduct.isPresent()) {
            Product existedProduct = optionalProduct.get();
            if (product.getId() == 0 || product.getId() != existedProduct.getId()) {
                errors.rejectValue(
                        "article",
                        "",
                        String.format("Product with article %s already exists", product.getArticle())
                );
            }
        }
    }
}
