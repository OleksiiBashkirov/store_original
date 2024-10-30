package bashkirov.store_original.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TelegramUser {
    private int id;

    private long chatId;

    private String username;

    private String phone;

    private String name;

    private String lastname;
}
