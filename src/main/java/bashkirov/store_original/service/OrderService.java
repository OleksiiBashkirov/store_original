package bashkirov.store_original.service;

import bashkirov.store_original.dto.OrderCartItemsDto;
import bashkirov.store_original.enumeration.OrdersStatus;
import bashkirov.store_original.model.CartItem;
import bashkirov.store_original.model.Orders;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.security.PersonDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final JdbcTemplate jdbcTemplate;
    private final CartItemService cartItemService;
    private final ProductService productService;

    public Orders getById(int orderId) {
        return jdbcTemplate.query(
                "select * from orders where id = ?",
                new Object[]{orderId},
                new BeanPropertyRowMapper<>(Orders.class)
        ).stream().findAny().orElseThrow(() -> new NoSuchElementException("Failed to find order by id=" + orderId));
    }

    public List<Orders> getAllByStatus(OrdersStatus status) {
        return jdbcTemplate.query(
                "select * from orders where status = ?",
                new Object[]{status.toString()},
                new BeanPropertyRowMapper<>(Orders.class)
        );
    }

    public List<OrderCartItemsDto> getAllByUser() {
        Person person = getCurrentUser();
        List<Orders> orderList = jdbcTemplate.query(
                "select * from orders where person_id = ? order by created_at DESC",
                new Object[]{person.getId()},
                new BeanPropertyRowMapper<>(Orders.class)
        );

        List<OrderCartItemsDto> orderCartItemsDtoList = new ArrayList<>();
        for (Orders order : orderList) {
            List<CartItem> cartItemList = cartItemService.getAllByOrderId(order.getId());
            orderCartItemsDtoList.add(new OrderCartItemsDto(order, cartItemList));
        }
        return orderCartItemsDtoList;
    }

    public List<Orders> getAll() {
        return jdbcTemplate.query(
                "select * from orders",
                new BeanPropertyRowMapper<>(Orders.class)
        );
    }

    public void createOrder(Orders order) {
        Person person = getCurrentUser();
        jdbcTemplate.update(
                "insert into orders(person_id, status, created_at, delivery_address, comment, name, lastname, phone) values (?,?,?,?,?,?,?,?)",
                person.getId(),
                OrdersStatus.PENDING_PAYMENT.toString(),
                LocalDateTime.now(),
                order.getDeliveryAddress(),
                order.getComment(),
                order.getName(),
                order.getLastname(),
                order.getPhone()
        );

        Orders orderLast = jdbcTemplate.query(
                        "select * from orders where person_id = ? order by created_at desc LIMIT 1",
                        new Object[]{person.getId()},
                        new BeanPropertyRowMapper<>(Orders.class)
                ).stream().findAny()
                .orElseThrow(() -> new NoSuchElementException("Failed to find any order by personId= " + person.getId()));

        List<CartItem> cartItemListOrderNull = cartItemService.getAllNotTaken();

        for (CartItem cartItem : cartItemListOrderNull) {
            Product product = productService.getById(cartItem.getProductId());
            if (product.getCountLeft() >= cartItem.getQuantity()) {
                jdbcTemplate.update(
                        "update product set count_left = ? where id = ?",
                        (product.getCountLeft() - cartItem.getQuantity()),
                        cartItem.getProductId()
                );
                jdbcTemplate.update(
                        "update cart_item set order_id = ? where id = ?",
                        orderLast.getId(),
                        cartItem.getId()
                );
                //якщо даного продукта залишилось хотяб 1 штука ми в корзині
                // ставим кількість рівну залишку інакше видаляєм з корзини
            } else if (product.getCountLeft() > 0) {
                jdbcTemplate.update(
                        "update cart_item set quantity = ? where id = ?",
                        product.getCountLeft(),
                        cartItem.getId()
                );
            } else {
                cartItemService.delete(cartItem.getId());
            }
        }
    }

    public void updateOrderStatus(int orderId, OrdersStatus status) {
        jdbcTemplate.update(
                "update orders set status = ? where id = ?",
                status.toString(),
                orderId
        );
    }

    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidOrders() {
        LocalDateTime cancelledDateTime = LocalDateTime.now().minusMinutes(15);

        List<Orders> ordersList = jdbcTemplate.query(
                "select * from orders where status = ? and created_at < ?",
                new Object[]{OrdersStatus.PENDING_PAYMENT.toString(), cancelledDateTime},
                new BeanPropertyRowMapper<>(Orders.class)
        );

        for (Orders order : ordersList) {
            List<CartItem> cartItems = jdbcTemplate.query(
                    "select * from cart_item where order_id = ?",
                    new Object[]{order.getId()},
                    new BeanPropertyRowMapper<>(CartItem.class)
            );

            for (CartItem cartItem : cartItems) {
                Product product = productService.getById(cartItem.getProductId());
                jdbcTemplate.update(
                        "update product set count_left = ? where id = ?",
                        product.getCountLeft() + cartItem.getQuantity(),
                        cartItem.getProductId()
                );

                jdbcTemplate.update(
                        "update cart_item set order_id = NULL where id = ?",
                        cartItem.getId()
                );
            }

            jdbcTemplate.update(
                    "delete from orders where id = ?",
                    order.getId()
            );
        }
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
