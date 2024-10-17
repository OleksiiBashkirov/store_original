package bashkirov.store_original.validation;

import bashkirov.store_original.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryValidator implements Validator {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean supports(Class<?> clazz) {
        return Objects.equals(clazz, Category.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Category category = (Category) target;
        Optional<Category> optionalCategory = jdbcTemplate.query(
                "select * from category where name = ?",
                new Object[]{category.getName()},
                new BeanPropertyRowMapper<>(Category.class)
        ).stream().findAny();
        if (optionalCategory.isPresent()) {
            errors.rejectValue(
                    "name",
                    "",
                    String.format("Category with name %s already exists", category.getName())
            );
        }

    }
}
