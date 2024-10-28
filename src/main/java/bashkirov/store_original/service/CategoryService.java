package bashkirov.store_original.service;

import bashkirov.store_original.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final JdbcTemplate jdbcTemplate;

    public List<Category> getAll() {
        return jdbcTemplate.query(
                "select * from category order by id",
                new BeanPropertyRowMapper<>(Category.class)
        );
    }

    public void save(Category category) {
        jdbcTemplate.update(
                "insert into category(name) values (?)",
                category.getName()
        );
    }
}
