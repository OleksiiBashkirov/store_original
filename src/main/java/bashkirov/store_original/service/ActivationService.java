package bashkirov.store_original.service;

import bashkirov.store_original.exception.InvalidActivationKeyException;
import bashkirov.store_original.model.Activation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivationService {
    private final JdbcTemplate jdbcTemplate;

    public void save(Activation activation) {
        jdbcTemplate.update(
                "insert into activation(key, email) values(?,?)",
                activation.getKey(),
                activation.getEmail()
        );
    }

    public String generateKey(String email) {
        StringBuilder sb = new StringBuilder();
        String alphabet = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
        int randomKeyLength = (int) (15 + (Math.random() * 6));
        for (int i = 0; i < randomKeyLength; i++) {
            int randomIndex = (int) (alphabet.length() * Math.random());
            sb.append(alphabet.charAt(randomIndex));
        }
        save(new Activation(sb.toString(), email));
        return sb.toString();
    }

    public void activate(String key) {
        Optional<Activation> optionalActivation = jdbcTemplate.query(
                "select * from activation where key = ?",
                new Object[]{key},
                new BeanPropertyRowMapper<>(Activation.class)
        ).stream().findAny();

        if (optionalActivation.isEmpty()) {
            throw new InvalidActivationKeyException("Failed to activate account with key=" + key);
        }

        jdbcTemplate.update(
                "update person set is_enable = true where email = ?",
                optionalActivation.get().getEmail()
        );

        jdbcTemplate.update(
                "delete from activation where key = ?",
                optionalActivation.get().getKey()
        );
    }
}
