package bashkirov.store_original.service;

import bashkirov.store_original.model.TelegramUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramUserService {
    private final JdbcTemplate jdbcTemplate;

    public Optional<TelegramUser> getByChatId(long chatId) {
        return jdbcTemplate.query(
                "select * from telegram_user where chat_id = ?",
                new Object[]{chatId},
                new BeanPropertyRowMapper<>(TelegramUser.class)
        ).stream().findAny();
    }

    public TelegramUser getById(int id) {
        return jdbcTemplate.query(
                "select * from telegram_user where id = ?",
                new Object[]{id},
                new BeanPropertyRowMapper<>(TelegramUser.class)
        ).stream().findAny().orElseThrow();
    }

    public List<TelegramUser> getAll() {
        return jdbcTemplate.query(
                "select * from telegram_user order by id desc",
                new BeanPropertyRowMapper<>(TelegramUser.class)
        );
    }

    public void save(TelegramUser telegramUser) {
        jdbcTemplate.update(
                "insert into telegram_user(chat_id, username, phone, name, lastname) values (?,?,?,?,?)",
                telegramUser.getChatId(),
                telegramUser.getUsername(),
                telegramUser.getPhone(),
                telegramUser.getName(),
                telegramUser.getLastname()
        );
    }

    public void deleteById(int id) {
        jdbcTemplate.update(
                "delete from telegram_user where id = ?",
                id
        );
    }

    public void deleteByChatId(long chatId) {
        jdbcTemplate.update(
                "delete from telegram_user where chat_id = ?",
                chatId
        );
    }

    public void addTelegramUserPhoneNumberByChatId(long chatId, String phone) {
        jdbcTemplate.update(
                "update telegram_user set phone = ? where chat_id = ?",
                phone,
                chatId
        );
    }
}
