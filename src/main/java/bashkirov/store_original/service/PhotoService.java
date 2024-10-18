package bashkirov.store_original.service;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import lombok.RequiredArgsConstructor;
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

    public ProductPhoto getPrimaryPhotoByProductId(int productId) {
        return jdbcTemplate.query(
                "select * from product_photo where product_id = ? and is_primary = true",
                new Object[]{productId},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        ).stream().findAny().orElseThrow(
                () -> new NoSuchElementException("Failed to find photo with id = " + productId)
        );
    }

    public List<ProductPhoto> getAllPhotoByProductId(int id) {
        return jdbcTemplate.query(
                "select * from product_photo where product_id = ? AND is_primary = false",
                new Object[]{id},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        );
    }

    public List<ProductPhoto> getAll() {
        return jdbcTemplate.query(
                "select * from product_photo",
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        );
    }

    public void save(MultipartFile multipartFile, Product product, boolean isPrimary) {
        String extension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        String filename = FilenameUtils.removeExtension(multipartFile.getOriginalFilename());
        String key = "photo/" + product.getArticle() + "/" + filename + "." + extension;
        doService.saveFile(multipartFile, key);

        ProductPhoto photo = new ProductPhoto();
        photo.setName(filename);
        photo.setUrl(key);
        photo.setPrimary(isPrimary);

        jdbcTemplate.update(
                "insert into product_photo(name, url, product_id, is_primary) values (?,?,?,?)",
                photo.getName(),
                photo.getUrl(),
                product.getId(),
                isPrimary
        );
    }

    public void saveAll(MultipartFile[] multipartFiles, Product product) {
        for (MultipartFile multipartFile : multipartFiles) {
            save(multipartFile, product, false);
        }
    }

    public void setPrimary(int photoId, int productId) {
        jdbcTemplate.update(
                "update product_photo set is_primary = false where product_id = ?",
                productId
        );

        jdbcTemplate.update(
                "update product_photo set is_primary = true where id = ?",
                photoId
        );
    }

    public void delete(int photoId) {
        jdbcTemplate.update(
                "delete from product_photo where id = ?",
                photoId
        );
    }
}
