package bashkirov.store_original.service;

import bashkirov.store_original.dto.EmailDto;
import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ActivationService activationService;
    private final EmailService emailService;

    public void register(Person person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole(Role.ROLE_USER);
        person.setEnable(false);

        String key = activationService.generateKey(person.getEmail());
        emailService.sendEmail(new EmailDto(
                person.getEmail(),
                "Activation key",
                "To activate account please follow the link\nhttp://localhost:8080/activate/" + key
        ));

        jdbcTemplate.update(
                "insert into person(name, lastname, address, phone, email, username, password, role, is_enable) values(?,?,?,?,?,?,?,?,?)",
                person.getName(),
                person.getLastname(),
                person.getAddress(),
                person.getPhone(),
                person.getEmail(),
                person.getUsername(),
                person.getPassword(),
                person.getRole().toString(),
                person.isEnable()
        );
    }
}
