package bashkirov.store_original.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TelegramCorrespondence {
    private int id;
    private long chatId;
    private boolean isAdmin;
    private String message;
    private LocalDateTime localDateTime;

    public TelegramCorrespondence(long chatId, boolean isAdmin, String message, LocalDateTime localDateTime) {
        this.chatId = chatId;
        this.isAdmin = isAdmin;
        this.message = message;
        this.localDateTime = localDateTime;
    }
}
