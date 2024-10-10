package bashkirov.store_original.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Activation {
    private int id;

    private String key;

    private String email;

    public Activation(String key, String email) {
        this.key = key;
        this.email = email;
    }
}
