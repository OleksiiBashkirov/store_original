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
import java.util.Optional;

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
        Optional<Wishlist> wishlist = jdbcTemplate.query(
                "select * from wishlist where product_id = ?",
                new Object[]{productId},
//                new BeanPropertyRowMapper<>(Wishlist.class)
                (rs, rowNum) -> {
                    Wishlist wishlist1 = new Wishlist();
                    wishlist1.setId(rs.getInt("id"));
                    wishlist1.setPersonId(rs.getInt("person_id"));
                    wishlist1.setProductId(rs.getInt("product_id"));
                    return wishlist1;
                }
        ).stream().findAny();

        if (wishlist.isPresent()) {
            System.out.println("Product with id=" + productId + " already exists in Wishlist");
            return;
        }

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
