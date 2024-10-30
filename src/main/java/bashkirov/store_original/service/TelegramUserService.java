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
    //дістати за чатАйді, зберегти, видалити, дістати всіх

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
                "select * from telegram_user order by id",
                new BeanPropertyRowMapper<>(TelegramUser.class)
        );
    }

    public void save(TelegramUser telegramUser) {
        jdbcTemplate.update(
                "insert into telegram_user(chat_id, username, phone, name, lastname) values (?,?,?,?,?)",
                telegramUser.getChatId(),
                telegramUser.getUsername(),
                telegramUser.getPhone(),
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
}
