package bashkirov.store_original.service;

import bashkirov.store_original.model.OrderPhoneCall;
import bashkirov.store_original.model.TelegramUser;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderPhoneCallService {
    private final JdbcTemplate jdbcTemplate;

    public List<OrderPhoneCall> getAllPhoneOrders() {
        return jdbcTemplate.query(
                "select * from order_phone_call order by date desc",
                getOrderPhoneCallRowMapper()
        );
    }

    public OrderPhoneCall getById(int id) {
        return jdbcTemplate.query(
                "select * from order_phone_call where id = ?",
                new Object[]{id},
                getOrderPhoneCallRowMapper()
        ).stream().findAny().orElseThrow();
    }

    public void save(TelegramUser telegramUser) {
        jdbcTemplate.update(
                "insert into order_phone_call(chat_id, phone, date, username, name, lastname, is_called) values (?,?,?,?,?,?,?)",
                telegramUser.getChatId(),
                telegramUser.getPhone(),
                LocalDateTime.now(),
                telegramUser.getUsername(),
                telegramUser.getName(),
                telegramUser.getLastname(),
                false
        );
    }

    public void update(int id, boolean isCalled) {
        jdbcTemplate.update(
                "update order_phone_call set is_called = ? where id =?",
                isCalled,
                id
        );
    }

    public void deleteById(int id) {
        jdbcTemplate.update(
                "delete from order_phone_call where id = ?",
                id
        );
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void autoDeleteAfterSevenDays() {
        jdbcTemplate.update(
                "delete from order_phone_call where date < (NOW() - interval '7 days') and is_called = true");
    }

    @NotNull
    private static RowMapper<OrderPhoneCall> getOrderPhoneCallRowMapper() {
        return (rs, rowNum) -> {
            OrderPhoneCall orderPhoneCall = new OrderPhoneCall();
            orderPhoneCall.setId(rs.getInt("id"));
            orderPhoneCall.setChatId(rs.getLong("chat_id"));
            orderPhoneCall.setPhone(rs.getString("phone"));
            orderPhoneCall.setDate(rs.getTimestamp("date").toLocalDateTime());
            orderPhoneCall.setUsername(rs.getString("username"));
            orderPhoneCall.setName(rs.getString("name"));
            orderPhoneCall.setLastname(rs.getString("lastname"));
            orderPhoneCall.setCalled(rs.getBoolean("is_called"));
            return orderPhoneCall;
        };
    }
}
