package bashkirov.store_original.service;

import bashkirov.store_original.model.TelegramCorrespondence;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramCorrespondenceService {
    private final JdbcTemplate jdbcTemplate;

    public void save(TelegramCorrespondence telegramCorrespondence) {
        jdbcTemplate.update(
                "insert into telegram_correspondence(chat_id, is_admin, message, date) values (?,?,?,?)",
                telegramCorrespondence.getChatId(),
                telegramCorrespondence.isAdmin(),
                telegramCorrespondence.getMessage(),
                LocalDateTime.now()
        );
    }

    public List<TelegramCorrespondence> getAllCorrespondenceByChatId(long chatId) {
        return jdbcTemplate.query(
                "select * from telegram_correspondence where chat_id = ? order by date desc",
                new Object[]{chatId},
                getTelegramCorrespondenceRowMapper()
        );
    }

    @NotNull
    private static RowMapper<TelegramCorrespondence> getTelegramCorrespondenceRowMapper() {
        return (rs, rowNum) -> {
            TelegramCorrespondence telegramCorrespondence = new TelegramCorrespondence();
            telegramCorrespondence.setChatId(rs.getLong("chat_id"));
            telegramCorrespondence.setAdmin(rs.getBoolean("is_admin"));
            telegramCorrespondence.setMessage(rs.getString("message"));
            telegramCorrespondence.setLocalDateTime(rs.getTimestamp("date").toLocalDateTime());
            return telegramCorrespondence;
        };
    }


}
