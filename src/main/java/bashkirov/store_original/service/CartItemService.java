package bashkirov.store_original.service;

import bashkirov.store_original.exception.AccessDeniedException;
import bashkirov.store_original.exception.CartItemNotFoundException;
import bashkirov.store_original.model.CartItem;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.security.PersonDetails;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartItemService {
    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;

    public void delete(int cartItemId) {
        Person person = getCurrentUser();
        CartItem cartItem = jdbcTemplate.query(
                "select * from cart_item where person_id = ?",
                new Object[]{person.getId()},
                new BeanPropertyRowMapper<>(CartItem.class)
        ).stream().findAny().orElseThrow(
                () -> new CartItemNotFoundException("Cart item not found by person id=" + person.getId())
        );

        if (cartItem.getPersonId() == person.getId()) {
            jdbcTemplate.update(
                    "delete from cart_item where id = ?",
                    cartItem.getId()
            );
        } else {
            throw new AccessDeniedException("This cartItem with id= " + cartItemId + " not belongs to you");
        }
    }

    public List<CartItem> getAllNotTaken() {
        Person person = getCurrentUser();
        return jdbcTemplate.query(
                "select * from cart_item where person_id = ? and order_id IS NULL",
                new Object[]{person.getId()},
                new BeanPropertyRowMapper<>(CartItem.class)
        );
    }

    public void add(int productId, int quantity) {
        Product product = jdbcTemplate.query(
                "select * from product where id = ?",
                new Object[]{productId},
                new BeanPropertyRowMapper<>(Product.class)
        ).stream().findAny().orElseThrow(
                () -> new NoSuchElementException("Failed to find product by id = " + productId)
        );

        if ((product.getCountLeft() - quantity) < 0) {
            throw new IllegalArgumentException("Product left = " + product.getCountLeft() + ". You should order this quantity or less");
        }

        Person person = getCurrentUser();
        jdbcTemplate.update(
                "insert into cart_item(person_id, product_id, quantity, order_id) values (?,?,?,?)",
                person.getId(),
                product.getId(),
                quantity,
                null
        );
    }

    public void update(int cartItemId, int quantityNew) {
        Optional<CartItem> optionalCartItem = jdbcTemplate.query(
                "select * from cart_item where id = ?",
                new Object[]{cartItemId},
                new BeanPropertyRowMapper<>(CartItem.class)
        ).stream().findAny();

        Person person = getCurrentUser();
        if (optionalCartItem.isPresent()) {
            CartItem cartItem = optionalCartItem.get();
            if (cartItem.getPersonId() != person.getId()) {
                throw new IllegalArgumentException("This cartItem not belongs to person with id =" + person.getId());
            }
            Product product = jdbcTemplate.query(
                    "select * from product where id = ?",
                    new Object[]{cartItem.getProductId()},
                    new BeanPropertyRowMapper<>(Product.class)
            ).stream().findAny().orElseThrow(() -> new NoSuchElementException("Failed to find product by productId=" + cartItem.getProductId()));

            if ((product.getCountLeft() - quantityNew) < 0) {
                throw new IllegalArgumentException("Product left = " + product.getCountLeft() + ". You should order this quantity or less");
            }

            if (quantityNew < 0) {
                delete(cartItem.getId());
            }
        }
    }

    public Person getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            PersonDetails userDetails = (PersonDetails) authentication.getPrincipal();
            return userDetails.person();
        }
        return null;
    }

    // якщо продукт який в карт айтемі в якого ордер_ід null і
    // його кількість більша ніж товарів в наявності
    // то ми йому зменшуємо кількість товарів  в корзині або видаляєм
    // цей карт айтем якщо товару не залишилось більше
    @Scheduled(fixedRate = 60000)
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
}
