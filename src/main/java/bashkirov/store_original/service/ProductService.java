package bashkirov.store_original.service;

import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.dto.ProductPhotosDto;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
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

    public void save(Product product, MultipartFile file, boolean isPrimary, MultipartFile[] multipartFiles) {
        product.setArticle(generateArticle());
//      збергіаємо продукт
        jdbcTemplate.update(
                "insert into product(title, price, article, count_left, description, category_id) values (?,?,?,?,?, ?)",
                product.getTitle(),
                product.getPrice(),
                product.getArticle(),
                product.getCountLeft(),
                product.getDescription(),
                product.getCategoryId()
        );
        product = getProductByArticle(product.getArticle());
        photoService.save(file, product, isPrimary);
        photoService.saveAll(multipartFiles, product);
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

    public static RowMapper<Product> getProductRowMapper() {
        return (rs, rowNum) -> {
            Product product = new Product();
            product.setId(rs.getInt("id"));
            product.setTitle(rs.getString("title"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setArticle(rs.getString("article"));
            product.setCountLeft(rs.getInt("count_left"));
            product.setDescription(rs.getString("description"));
            product.setCategoryId(rs.getInt("category_id"));
            return product;
        };
    }

    public Product getById(int id) {
        return jdbcTemplate.query(
                "select * from product where id = ?",
                new Object[]{id},
                new BeanPropertyRowMapper<>(Product.class)
        ).stream().findAny().orElseThrow(() -> new NoSuchElementException("Failed to get product by id=" + id));
    }

    public List<Product> getAll() {
        return jdbcTemplate.query(
                "select p.* from product p join category c on p.category_id=c.id order by p.id",
                ProductService.getProductRowMapper()
        );
    }

    public List<ProductPhotoDto> getAllProductPhotos() {
        List<Product> productList = getAll();
        return transformProductToProductDto(productList);
    }

    public List<ProductPhoto> getAllPhotoByProductId(int productId) {
        return jdbcTemplate.query(
                "select * from product_photo where product_id = ? order by product_id",
                new Object[]{productId},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        );
    }

    private List<ProductPhotoDto> transformProductToProductDto(List<Product> productList) {
        List<ProductPhotoDto> productPhotoDtoList = new ArrayList<>();
        for (Product product : productList) {
            ProductPhoto productPhoto = photoService.getPrimaryPhotoByProductId(product.getId());
            productPhotoDtoList.add(new ProductPhotoDto(product, productPhoto));
        }
        return productPhotoDtoList;
    }

    public ProductPhotosDto getProductWithPhotos(int productId) {
        Product product = getById(productId);
        ProductPhoto primaryProductPhoto = photoService.getPrimaryPhotoByProductId(product.getId());
        List<ProductPhoto> productPhotoList = photoService.getAllPhotoByProductId(product.getId());
        return new ProductPhotosDto(product, primaryProductPhoto, productPhotoList);
    }

    public void update(int id, Product product) {
        jdbcTemplate.update(
                "update product set title = ?, price = ?, article = ?, count_left = ?, description = ?, category_id = ? where id = ?",
                product.getTitle(),
                product.getPrice(),
                product.getArticle(),
                product.getCountLeft(),
                product.getDescription(),
                product.getCategoryId(),
                id
        );
    }

    public void delete(int id) {
        jdbcTemplate.update(
                "delete from product where id = ?",
                id
        );
    }

    public List<ProductPhotoDto> search(String key, Integer categoryId) {
        if (key == null || key.isBlank()) {
            if (categoryId != null) {
                return getAllByCategoryId(categoryId);
            }
            return getAllProductPhotos();
        }
        String iLikeQuery = "%" + key + "%";

        List<Product> productList = null;
        if (categoryId == null) {
            productList = jdbcTemplate.query(
                    "select * from product where title ILIKE ? OR article ILIKE ? OR description ILIKE ? order by id",
                    new Object[]{iLikeQuery, iLikeQuery, iLikeQuery},
                    new BeanPropertyRowMapper<>(Product.class)
            );
        } else {
            productList = jdbcTemplate.query(
                    "select * from product where title ILIKE ? OR article ILIKE ? OR description ILIKE ? AND category_id = ? order by id",
                    new Object[]{iLikeQuery, iLikeQuery, iLikeQuery, categoryId},
                    new BeanPropertyRowMapper<>(Product.class)
            );
        }

        return transformProductToProductDto(productList);
    }

    public List<ProductPhotoDto> getAllByCategoryId(int categoryId) {

        List<Product> query = jdbcTemplate.query(
                "select * from product where category_id = ? order by id",
                new Object[]{categoryId},
                new BeanPropertyRowMapper<>(Product.class)
        );
        return transformProductToProductDto(query);
    }
}
