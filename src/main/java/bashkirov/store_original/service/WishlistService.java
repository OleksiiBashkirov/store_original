package bashkirov.store_original.service;

import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.dto.WishlistDto;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import bashkirov.store_original.model.Wishlist;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final JdbcTemplate jdbcTemplate;
    private final PersonDetailsService personDetailsService;
    private final ProductService productService;
    private final PhotoService photoService;

    public List<WishlistDto> getWishlistListDtoByPersonId(int personId) {
        List<Wishlist> wishlistList = jdbcTemplate.query(
                "select * from wishlist where person_id = ?",
                new Object[]{personId},
                new BeanPropertyRowMapper<>(Wishlist.class)
        );

        List<WishlistDto> wishlistDtoList = new ArrayList<>();
        for (Wishlist wishlist : wishlistList) {
            Product product = productService.getById(wishlist.getProductId());
            ProductPhoto productPhoto = productService.getAllPhotoByProductId(wishlist.getProductId()).getFirst();
            ProductPhotoDto productPhotoDto = new ProductPhotoDto(product, productPhoto);
            wishlistDtoList.add(new WishlistDto(wishlist, productPhotoDto));
        }
        return wishlistDtoList;
    }

    public void addProductToWishList(int productId) {
        Person person = personDetailsService.getCurrentUser();
        jdbcTemplate.update(
                "insert into wishlist(person_id, product_id) values (?,?)",
                person.getId(),
                productId
        );
    }

    public void removeProductFromWishlist(int productId) {
        Person person = personDetailsService.getCurrentUser();
        jdbcTemplate.update(
                "delete from wishlist where person_id = ? AND product_id = ?",
                person.getId(),
                productId
        );
    }
}
