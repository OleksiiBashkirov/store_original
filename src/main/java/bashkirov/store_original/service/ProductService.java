package bashkirov.store_original.service;

import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.dto.ProductPhotosDto;
import bashkirov.store_original.dto.ProductSaleDto;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import bashkirov.store_original.model.PromoCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

//генеруємо артикл для продукту, зберігаєм продукт , створюєм ключ, зберігаєм фотку
@Service
@RequiredArgsConstructor
public class ProductService {
    private static final int ARTICLE_LENGTH = 12;

    private final JdbcTemplate jdbcTemplate;
    private final PhotoService photoService;
    private final PromoCodeService promoCodeService;

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
            product.setPrice(rs.getDouble("price"));
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

    public List<Product> getAll(int page, int size) {
        return jdbcTemplate.query(
                "select p.* from product p join category c on p.category_id=c.id order by p.id LIMIT ? OFFSET ?",
                new Object[]{size, (page * size)},
                ProductService.getProductRowMapper()
        );
    }

    public List<ProductPhotoDto> getAllProductPhotos(int page, int size) {
        List<Product> productList = getAll(page, size);
        return transformProductToProductPhotoDto(productList);
    }

    public List<ProductPhoto> getAllPhotoByProductId(int productId) {
        return jdbcTemplate.query(
                "select * from product_photo where product_id = ? order by product_id",
                new Object[]{productId},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        );
    }

    private List<ProductPhotoDto> transformProductToProductPhotoDto(List<Product> productList) {
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
                "update product set title = ?, price = ?, count_left = ?, description = ?, category_id = ? where id = ?",
                product.getTitle(),
                product.getPrice(),
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

    public List<ProductPhotoDto> search(String key, Integer categoryId, int page, int size) {
        if (key == null || key.isBlank()) {
            if (categoryId != null) {
                return getAllByCategoryId(categoryId, page, size);
            }
            return getAllProductPhotos(page, size);
        }
        String iLikeQuery = "%" + key + "%";

        List<Product> productList = null;
        if (categoryId == null) {
            productList = jdbcTemplate.query(
                    "select * from product where title ILIKE ? OR article ILIKE ? OR description ILIKE ? order by id LIMIT ? OFFSET ?",
                    new Object[]{iLikeQuery, iLikeQuery, iLikeQuery, size, (page * size)},
                    new BeanPropertyRowMapper<>(Product.class)
            );
        } else {
            productList = jdbcTemplate.query(
                    "select * from product where title ILIKE ? OR article ILIKE ? OR description ILIKE ? AND category_id = ? order by id LIMIT ? OFFSET ?",
                    new Object[]{iLikeQuery, iLikeQuery, iLikeQuery, categoryId, size, (page * size)},
                    new BeanPropertyRowMapper<>(Product.class)
            );
        }

        return transformProductToProductPhotoDto(productList);
    }

    public List<ProductPhotoDto> getAllProductsWithDiscountInfo(int page, int size) {
        List<ProductPhotoDto> productPhotoDtos = getAllProductPhotos(page, size);
        productPhotoDtos.forEach(productPhotoDto -> {
            getOptionalProductSaleDto(productPhotoDto.getProduct().getId()).ifPresent(
                    productSaleDto -> {
                        productPhotoDto.getProduct().setSalePrice(productSaleDto.getSalePrice());
                        productPhotoDto.getProduct().setDateExpired(productSaleDto.getProductPhotoDto().getProduct().getDateExpired());
                    }
            );
        });

        return productPhotoDtos;
    }

    public List<ProductPhotoDto> searchWithDiscount(String key, Integer categoryId, int page, int size) {
        List<ProductPhotoDto> productPhotoDtos = search(key, categoryId, page, size);

        productPhotoDtos.forEach(productPhotoDto -> {
            getOptionalProductSaleDto(productPhotoDto.getProduct().getId()).ifPresent(
                    productSaleDto -> {
                        productPhotoDto.getProduct().setSalePrice(productSaleDto.getSalePrice());
                        productPhotoDto.getProduct().setDateExpired(productSaleDto.getProductPhotoDto().getProduct().getDateExpired());
                    }
            );
        });

        return productPhotoDtos;
    }

    public List<ProductPhotoDto> getAllByCategoryId(int categoryId, int page, int size) {

        List<Product> query = jdbcTemplate.query(
                "select * from product where category_id = ? order by id LIMIT ? OFFSET ?",
                new Object[]{categoryId, size, (page * size)},
                new BeanPropertyRowMapper<>(Product.class)
        );
        return transformProductToProductPhotoDto(query);
    }

    public int countProducts() {
        Integer count = jdbcTemplate.queryForObject(
                "select COUNT(*) from product",
                Integer.class
        );
        return count == null ? 0 : count;
    }

    public int countProductsByCategory(Integer categoryId) {
        Integer countByCategory = jdbcTemplate.queryForObject(
                "select COUNT(*) from product where category_id = ?",
                new Object[]{categoryId},
                Integer.class
        );
        return countByCategory == null ? 0 : countByCategory;
    }

    public void addSaleProduct(ProductSaleDto productSaleDto) {
        Product product = getById(productSaleDto.getProductPhotoDto().getProduct().getId());
        LocalDateTime dateExpired = LocalDateTime.now().plusHours(productSaleDto.getHours());

        jdbcTemplate.update(
                "update product set sale_price = ?, date_expired = ? where id = ?",
                productSaleDto.getSalePrice(),
                dateExpired,
                product.getId()
        );
    }

    public void cancelSaleProduct(int productId) {
        jdbcTemplate.update(
                "update product set sale_price = NULL , date_expired = NULL where id = ?",
                productId
        );
    }

    public Optional<ProductSaleDto> getOptionalProductSaleDto(int productId) {
        ProductPhotoDto productPhotoDto = getProductPhotoDtoByProductId(productId);

        if (productPhotoDto.getProduct().getSalePrice() != null &&
                productPhotoDto.getProduct().getDateExpired() != null &&
                productPhotoDto.getProduct().getDateExpired().isAfter(LocalDateTime.now())) {
            return Optional.of(new ProductSaleDto(
                    productPhotoDto,
                    productPhotoDto.getProduct().getSalePrice(),
                    (int) java.time.Duration.between(
                            LocalDateTime.now(),
                            productPhotoDto.getProduct().getDateExpired()).toHours()
            ));
        }
        return Optional.empty();
    }

    public List<ProductSaleDto> getAllProductSaleDto() {
        List<Product> productList = jdbcTemplate.query(
                "select * from product where sale_price IS NOT NULL AND date_expired > NOW() order by id",
                new BeanPropertyRowMapper<>(Product.class)
        );

        List<ProductPhotoDto> productPhotoDtos = transformProductToProductPhotoDto(productList);

        List<ProductSaleDto> productSaleDtoList = new ArrayList<>();
        for (ProductPhotoDto productPhotoDto : productPhotoDtos) {
            productSaleDtoList.add(new ProductSaleDto(
                    productPhotoDto,
                    productPhotoDto.getProduct().getSalePrice(),
                    (int) java.time.Duration.between(
                            LocalDateTime.now(),
                            productPhotoDto.getProduct().getDateExpired()).toHours()));
        }
        return productSaleDtoList;
    }

    public String getFiveRandomProductSaleDto() {
        List<Product> productList = jdbcTemplate.query(
                "select * from product where sale_price IS NOT NULL AND date_expired > NOW() order by id",
                new BeanPropertyRowMapper<>(Product.class)
        );

        int size = Math.min(productList.size(), 5);
        List<Product> randomProducts = new ArrayList<>();

        while (randomProducts.size() < size) {
            int randomIndex = (int) (Math.random() * productList.size());
            Product randomProduct = productList.get(randomIndex);
            if (!randomProducts.contains(randomProduct)) {
                randomProducts.add(randomProduct);
            }
        }

        List<ProductPhotoDto> productPhotoDtos = transformProductToProductPhotoDto(randomProducts);

        List<ProductSaleDto> fiveRandomProductSaleDtoList = new ArrayList<>();
        for (ProductPhotoDto productPhotoDto : productPhotoDtos) {
            fiveRandomProductSaleDtoList.add(new ProductSaleDto(
                    productPhotoDto,
                    productPhotoDto.getProduct().getSalePrice(),
                    (int) java.time.Duration.between(
                            LocalDateTime.now(),
                            productPhotoDto.getProduct().getDateExpired()).toHours()));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 *Акційні Пропозиції* 🔥\n\n");

        for (ProductSaleDto productSaleDto : fiveRandomProductSaleDtoList) {
            String productTitle = productSaleDto.getProductPhotoDto().getProduct().getTitle();
            String productUrl = "http://localhost:8080/product/" + productSaleDto.getProductPhotoDto().getProduct().getId();
            String productUrl2 = "https://www.google.com.ua/?hl=uk";
            String productUrl3 = "https://store.bashkirov.space/product/" + productSaleDto.getProductPhotoDto().getProduct().getId();
            String photoUrl = "https://bashkirovbank.fra1.digitaloceanspaces.com/" + productSaleDto.getProductPhotoDto().getProductPhoto().getUrl();

            sb.append("🛒 *Продукт*: ").append(productTitle).append("\n")
                    .append("📷 [Фото](").append(photoUrl).append(")\n")
                    .append("💲 *Стара ціна*: ").append(productSaleDto.getProductPhotoDto().getProduct().getPrice()).append(" грн\n")
                    .append("💥 *Акційна ціна*: ").append(productSaleDto.getSalePrice()).append(" грн\n")
                    .append("⏳ Діє ще: ").append(productSaleDto.getHours()).append(" годин\n")
                    .append("🔗 [Деталі продукту](").append(productUrl3).append(")\n\n");
        }
        return sb.toString();
    }

    public String getInfoAboutUs() {
        return """
                🏪 *BashkirovShop* – Ваш надійний партнер для вигідних покупок!
                
                📞 Контактний номер: +38 (073) 001-003-1
                🌐 Вебсайт: [bashkirov.shop](https://store.bashkirov.space/product)
                🕒 Графік роботи: Пн-Пт 9:00 - 18:00
                
                Ми раді допомогти вам з будь-якими питаннями!
                Дякуємо, що обираєте нас ❤️
                """;
    }


    public Double getActualPrice(Product product) {
        return getOptionalProductSaleDto(product.getId())
                .map(ProductSaleDto::getSalePrice)
                .orElse(product.getPrice());
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void autoFinishSaleProduct() {
        List<Product> products = jdbcTemplate.query(
                "select * from product where date_expired <= NOW()",
                new BeanPropertyRowMapper<>(Product.class)
        );

        for (Product product : products) {
            cancelSaleProduct(product.getId());
        }
    }

    public Double applyPromoCode(Product product, String code) {
        Optional<PromoCode> promoCodeOpt = promoCodeService.getValidPromoCode(code, product.getCategoryId());

        if (promoCodeOpt.isPresent()) {
            PromoCode promoCode = promoCodeOpt.get();
            double discount = promoCode.isPercentage() ?
                    (product.getPrice() * (promoCode.getDiscount() / 100)) :
                    promoCode.getDiscount();
            return ((int) (Math.max(1, product.getPrice() - discount) * 100)) / 100.;
        }
        return product.getPrice();
    }

    private ProductPhotoDto getProductPhotoDtoByProductId(int productId) {
        Product product = getById(productId);
        ProductPhoto productPhoto = jdbcTemplate.query(
                "select * from product_photo where product_id = ?",
                new Object[]{product.getId()},
                new BeanPropertyRowMapper<>(ProductPhoto.class)
        ).stream().findAny().orElseThrow();
        return new ProductPhotoDto(product, productPhoto);
    }
}
