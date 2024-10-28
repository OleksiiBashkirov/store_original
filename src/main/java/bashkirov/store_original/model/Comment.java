package bashkirov.store_original.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private int personId;

    private int productId;

    private String comment;

    private LocalDateTime createdAt;
}
