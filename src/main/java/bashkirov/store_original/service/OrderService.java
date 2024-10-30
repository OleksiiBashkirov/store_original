package bashkirov.store_original.service;

import bashkirov.store_original.dto.OrderCartItemsDto;
import bashkirov.store_original.enumeration.OrdersStatus;
import bashkirov.store_original.model.CartItem;
import bashkirov.store_original.model.Orders;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.model.Product;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final JdbcTemplate jdbcTemplate;
    private final CartItemService cartItemService;
    private final ProductService productService;
    private final PersonDetailsService personDetailsService;

    public Orders getById(int orderId) {
        return jdbcTemplate.query(
                "select * from orders where id = ?",
                new Object[]{orderId},
                getOrdersRowMapper()
        ).stream().findAny().orElseThrow(() -> new NoSuchElementException("Failed to find order by id=" + orderId));
    }

    public OrderCartItemsDto getWithCartItemsDtoById(int orderId) {
        Orders order = getById(orderId);
        List<CartItem> cartItemList = cartItemService.getAllByOrderId(orderId);

        return new OrderCartItemsDto(order, cartItemList);
    }

    public OrderCartItemsDto getLastUserOrderWithCartItemsDto(){
        List<OrderCartItemsDto> allByUser = getAllByUser();
        return allByUser.getFirst();
    }

    public List<Orders> getAllByStatus(OrdersStatus status) {
        return jdbcTemplate.query(
                "select * from orders where status = ? order by id DESC",
                new Object[]{status.toString()},
                getOrdersRowMapper()
        );
    }

    public List<OrderCartItemsDto> getAllByUser() {
        Person person = personDetailsService.getCurrentUser();
        List<Orders> orderList = jdbcTemplate.query(
                "select * from orders where person_id = ? order by created_at DESC",
                new Object[]{person.getId()},
                getOrdersRowMapper()
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
                "select * from orders order by id DESC",
                getOrdersRowMapper()
        );
    }

    public List<Orders> getAllSortedByOrderIdAsc() {
        return jdbcTemplate.query(
                "select * from orders order by id",
                getOrdersRowMapper()
        );
    }

    public List<Orders> getAllSortedByCreatedAtAsc() {
        return jdbcTemplate.query(
                "select * from orders order by created_at",
                getOrdersRowMapper()
        );
    }

    public List<Orders> getAllSortedByCreatedAtDesc() {
        return jdbcTemplate.query(
                "select * from orders order by created_at DESC",
                getOrdersRowMapper()
        );
    }

    public void createOrder(Orders order) {
        Person person = personDetailsService.getCurrentUser();
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

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void cancelUnpaidOrders() {
        System.out.println("TEST");
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

    private static RowMapper<Orders> getOrdersRowMapper() {
        return (rs, rowNum) -> {
            Orders order = new Orders();
            order.setId(rs.getInt("id"));
            order.setPersonId(rs.getInt("person_id"));
            order.setOrdersStatus(OrdersStatus.valueOf(rs.getString("status")));
            order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            order.setDeliveryAddress(rs.getString("delivery_address"));
            order.setComment(rs.getString("comment"));
            order.setName(rs.getString("name"));
            order.setLastname(rs.getString("lastname"));
            order.setPhone(rs.getString("phone"));
            return order;
        };
    }

    @NotNull
    public  Orders getForOrderPersonDetails() {
        Person person = personDetailsService.getCurrentUser();
        Orders order = new Orders();
        order.setName(person.getName());
        order.setLastname(person.getLastname());
        order.setPhone(person.getPhone());
        order.setDeliveryAddress(person.getAddress());
        return order;
    }
}
