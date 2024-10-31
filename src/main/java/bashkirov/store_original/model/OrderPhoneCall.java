package bashkirov.store_original.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPhoneCall {
    private int id;
    private long chatId;
    private String phone;
    private LocalDateTime date;
    private String username;
    private String name;
    private String lastname;
    private boolean isCalled;
}
