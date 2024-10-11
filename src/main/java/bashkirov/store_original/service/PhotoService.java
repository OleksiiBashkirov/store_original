package bashkirov.store_original.service;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final JdbcTemplate jdbcTemplate;
    private final DoService doService;
    @Setter
    private ProductService productService;

    public ProductPhoto getById(int id) {
        return jdbcTemplate.query(
                "select  * from product_photo where id = ?",
                new Object[]{id},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        ).stream().findAny().orElseThrow(
                () -> new NoSuchElementException("Failed to find photo with id =" + id)
        );
    }

    public List<ProductPhoto> getAll() {
        return jdbcTemplate.query(
                "select * from product_photo",
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        );
    }

    public void save(MultipartFile multipartFile, String article, boolean isPrimary) {
        String extension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        String filename = FilenameUtils.removeExtension(multipartFile.getOriginalFilename());
        String key = "photo/" + article + "/" + filename + "." + extension;
        doService.saveFile(multipartFile, key);

        // підключимо продукт сервіс який буде знаходити продукт за артиклом
        Product productByArticle = productService.getProductByArticle(article);

        ProductPhoto photo = new ProductPhoto();
        photo.setName(filename);
        photo.setUrl(key);
        photo.setPrimary(isPrimary);

        jdbcTemplate.update(
                "insert into product_photo(name, url, product_id, is_primary) values (?,?,?,?)",
                photo.getName(),
                photo.getUrl(),
                productByArticle.getId(),
                isPrimary
        );
    }

    public void saveAll(MultipartFile[] multipartFiles, Product product) {
        boolean isPrimary = true;

        for (MultipartFile multipartFile : multipartFiles) {
            if (!multipartFile.isEmpty()) {
                save(multipartFile, product.getArticle(), isPrimary);
                isPrimary = false;
            }
        }
    }

    public void update(int photoId, boolean isPrimary) {
        jdbcTemplate.update(
                "update product_photo set is_primary = ? where id = ?",
                isPrimary,
                photoId
        );
    }

    public void deleteById(int photoId) {
        jdbcTemplate.update(
                "delete from product_photo where id = ?",
                photoId
        );
    }
}
