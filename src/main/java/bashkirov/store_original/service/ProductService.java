package bashkirov.store_original.service;

import bashkirov.store_original.model.Category;
import bashkirov.store_original.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;
import java.util.Optional;

//генеруємо артикл для продукту , зберігаєм продукт , створюєм ключ, зберігаєм фотку,
@Service
@RequiredArgsConstructor
public class ProductService {
    private static final int ARTICLE_LENGTH = 12;

    private final JdbcTemplate jdbcTemplate;
    private final PhotoService photoService;

    public String generateArticle() {
        StringBuilder sb = null;
        String alphabet = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        boolean isUnique = false;

        while (!isUnique) {
            sb = new StringBuilder();
            for (int i = 0; i < ARTICLE_LENGTH; i++) {
                int randomIndex = (int) (alphabet.length() * Math.random());
                sb.append(alphabet.charAt(randomIndex));
            }
            Optional<Product> optionalProduct = jdbcTemplate.query(
                    "select * from product where article = ?",
                    new Object[]{sb.toString()},
                    new BeanPropertyRowMapper<>(Product.class)
            ).stream().findAny();
            if (!optionalProduct.isPresent()) {
                isUnique = true;
            }
        }
        return sb.toString();
    }

    public void save(Product product, MultipartFile file, boolean isPrimary) {
        product.setArticle(generateArticle());
//      збергіаємо продукт
        jdbcTemplate.update(
                "insert into product(title, price, article, count_left, description, category_id) values (?,?,?,?,?,?)",
                product.getTitle(),
                product.getPrice(),
                product.getArticle(),
                product.getCountLeft(),
                product.getDescription(),
                product.getCategory().getId()
        );

        photoService.save(file, product.getArticle(), isPrimary);
    }

    public Product getProductByArticle(String article) {
        return jdbcTemplate.query(
                        "select p.*, c.name from product p join category c on p.category_id = c.id where p.article = ?",
                        new Object[]{article},
                        getProductRowMapper()
                ).stream().findAny()
                .orElseThrow(
                        () -> new NoSuchElementException("Failed to find product with article=" + article)
                );
    }

    private static RowMapper<Product> getProductRowMapper() {
        return (rs, rowNum) -> {
            Product product = new Product();
            product.setId(rs.getInt("id"));
            product.setTitle(rs.getString("title"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setArticle(rs.getString("article"));
            product.setCountLeft(rs.getInt("count_left"));
            product.setDescription(rs.getString("description"));
            Category category = new Category();
            category.setId(rs.getInt("category_id"));
            category.setName(rs.getString("name"));
            product.setCategory(category);

            return product;
        };
    }
}
