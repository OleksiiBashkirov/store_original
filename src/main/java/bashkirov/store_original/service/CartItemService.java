package bashkirov.store_original.service;

import bashkirov.store_original.dto.CartItemDto;
import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.exception.AccessDeniedException;
import bashkirov.store_original.exception.CartItemNotFoundException;
import bashkirov.store_original.model.CartItem;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.model.ProductPhoto;
import bashkirov.store_original.security.PersonDetails;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartItemService {
//    private static final Person DEFAULT_USER = new Person(
//            6, // Default ID
//            "Anonymous", // Default name
//            "User", // Default lastname
//            "00000, Unknown, Anonymous Street", // Default address
//            "+0000000000", // Default phone
//            "anonymous@example.com", // Default email
//            "guest", // Default username
//            "", // No password needed
//            Role.ROLE_GUEST, // Define a guest role in your Role enum
//            true // Enable the default user
//    );

    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;
    private final PhotoService photoService;

    public void deleteCartItemById(int cartItemId) {
        Person person = getCurrentUser();
        CartItem cartItem = jdbcTemplate.query(
                "select * from cart_item where id = ? AND person_id = ?",
                new Object[]{cartItemId, person.getId()},
                new BeanPropertyRowMapper<>(CartItem.class)
        ).stream().findAny().orElseThrow(
                () -> new CartItemNotFoundException("Cart item not found by person id=" + person.getId())
        );

        if (cartItem.getPersonId() == person.getId()) {
            jdbcTemplate.update(
                    "delete from cart_item where id = ?",
                    cartItemId
            );
        } else {
            throw new AccessDeniedException("This cartItem with id= " + cartItemId + " not belongs to you");
        }
    }

    public List<CartItem> getAllCartItemsNotTaken() {
        Person person = getCurrentUser();
        return jdbcTemplate.query(
                "select * from cart_item where person_id = ? and order_id IS NULL",
                new Object[]{person.getId()},
                new BeanPropertyRowMapper<>(CartItem.class)
        );
    }

    public List<CartItemDto> getAllCartItemDtosInShoppingCart() {
        Person person = getCurrentUser();
        List<CartItem> cartItemList = jdbcTemplate.query(
                "select * from cart_item where person_id = ? and order_id IS NULL order by id",
                new Object[]{person.getId()},
                new BeanPropertyRowMapper<>(CartItem.class)
        );
        List<CartItemDto> cartItemDtoList = new ArrayList<>();

        for (CartItem item : cartItemList) {
            Product product = productService.getById(item.getProductId());
            ProductPhoto productPhoto = photoService.getPrimaryPhotoByProductId(item.getProductId());

            cartItemDtoList.add(new CartItemDto(item, new ProductPhotoDto(product, productPhoto)));
        }
        return cartItemDtoList;
    }

    public double getTotalPriceOffAllCartItemsInShoppingCart() {
        List<CartItemDto> allCartItemDtosInShoppingCart = getAllCartItemDtosInShoppingCart();
        double sum = 0;
        for (CartItemDto cartItemDto : allCartItemDtosInShoppingCart) {
            if (cartItemDto.getProductPhotoDto().getProduct().getSalePrice() != null) {
                sum += cartItemDto.getProductPhotoDto().getProduct().getSalePrice()
                        * cartItemDto.getCartItem().getQuantity();
            }
            sum += cartItemDto.getProductPhotoDto().getProduct().getPrice()
                    * cartItemDto.getCartItem().getQuantity();
        }
        return ((int) (sum * 100)) / 100.;
    }

    public void addCartItemByProductId(int productId) {
        Product product = jdbcTemplate.query(
                "select * from product where id = ?",
                new Object[]{productId},
                new BeanPropertyRowMapper<>(Product.class)
        ).stream().findAny().orElseThrow(
                () -> new NoSuchElementException("Failed to find product by id = " + productId)
        );

        if ((product.getCountLeft() - 1) < 0) {
            throw new IllegalArgumentException("Кількість товару на складі = " + product.getCountLeft() + ". Вибачте, товар закінчився або необхідно обрати кількість не більшу ніж на складі.");
        }

        Person person = getCurrentUser();

        if (isProductPresentInCart(productId)) {
            CartItem cartItemByProductId = getCartItemByProductId(productId);
            int addQuantity = 1;
            updateCartItemQuantity(
                    cartItemByProductId.getId(),
                    cartItemByProductId.getQuantity() + addQuantity
            );
        } else {
            jdbcTemplate.update(
                    "insert into cart_item(person_id, product_id, quantity, order_id) values (?,?,?,?)",
                    person.getId(),
                    product.getId(),
                    1,
                    null
            );
        }
    }

    @NotNull
    private CartItem getCartItemByProductId(int productId) {
        return jdbcTemplate.query(
                "select * from defaultdb.public.cart_item where product_id = ?",
                new Object[]{productId},
                getCartItemRowMapper()
        ).stream().findAny().orElseThrow();
    }

    public void updateCartItemQuantity(int cartItemId, int quantityNew) {
        Optional<CartItem> optionalCartItem = jdbcTemplate.query(
                "select * from cart_item where id = ?",
                new Object[]{cartItemId},
                getCartItemRowMapper()
        ).stream().findAny();

        Person person = getCurrentUser();
        if (optionalCartItem.isPresent()) {
            CartItem cartItem = optionalCartItem.get();
            if (cartItem.getPersonId() != person.getId()) {
                throw new IllegalArgumentException("This Item not belongs to person with id =" + person.getId());
            }
            Product product = jdbcTemplate.query(
                    "select * from product where id = ?",
                    new Object[]{cartItem.getProductId()},
                    new BeanPropertyRowMapper<>(Product.class)
            ).stream().findAny().orElseThrow(() -> new NoSuchElementException("Failed to find product by productId=" + cartItem.getProductId()));

            if ((product.getCountLeft() - quantityNew) <= 0) {
                throw new IllegalArgumentException("Product left = " + product.getCountLeft() + ". You should order this quantity or less");
            }

            if (quantityNew < 1) {
                deleteCartItemById(cartItem.getId());
                return;
            }

            jdbcTemplate.update(
                    "update cart_item set quantity = ? where id= ?",
                    quantityNew,
                    cartItemId
            );
        }
    }

    public boolean isProductPresentInCart(int productId) {
        Person person = getCurrentUser();
        if (person != null) {
            Optional<CartItem> optionalCartItem = jdbcTemplate.query(
                    "select * from cart_item where person_id = ? AND product_id = ?",
                    new Object[]{person.getId(), productId},
                    new BeanPropertyRowMapper<>(CartItem.class)
            ).stream().findAny();
            return optionalCartItem.isPresent();
        }
        return false;
    }

    public List<CartItem> getAllByOrderId(int orderId) {
        return jdbcTemplate.query(
                "select * from cart_item where order_id = ?",
                new Object[]{orderId},
                new BeanPropertyRowMapper<>(CartItem.class)
        );
    }

    // якщо продукт який в карт айтемі в якого ордер_ід null і
    // його кількість більша ніж товарів в наявності
    // то ми йому зменшуємо кількість товарів  в корзині або видаляєм
    // цей карт айтем якщо товару не залишилось більше
//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void validateProductQuantity() {

        List<CartItem> cartItemList = jdbcTemplate.query(
                "select * from cart_item where order_id IS NULL",
                new BeanPropertyRowMapper<>(CartItem.class)
        );

        for (CartItem cartItem : cartItemList) {
            Product product = productService.getById(cartItem.getProductId());
            if (product.getCountLeft() <= 0) {
                jdbcTemplate.update(
                        "delete from cart_item where id = ?",
                        cartItem.getId()
                );
            }

            if (cartItem.getQuantity() > product.getCountLeft()) {
                jdbcTemplate.update(
                        "update cart_item set quantity = ? where id = ?",
                        product.getCountLeft(),
                        cartItem.getId()
                );
            }
        }
    }


    private static RowMapper<CartItem> getCartItemRowMapper() {
        return (rs, rowNum) -> {
            CartItem cartItem = new CartItem();
            cartItem.setId(rs.getInt("id"));
            cartItem.setPersonId(rs.getInt("person_id"));
            cartItem.setProductId(rs.getInt("product_id"));
            cartItem.setQuantity(rs.getInt("quantity"));
            cartItem.setOrderId(rs.getInt("order_id"));
            return cartItem;
        };
    }

    private Person getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            PersonDetails userDetails = (PersonDetails) authentication.getPrincipal();
            return userDetails.person();
        }
        return null;
    }
}
