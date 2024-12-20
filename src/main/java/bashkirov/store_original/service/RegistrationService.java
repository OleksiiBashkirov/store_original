package bashkirov.store_original.service;

import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Person;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ActivationService activationService;
    private final EmailService emailService;
    private final PersonDetailsService personDetailsService;

    @SneakyThrows
    public void register(Person person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole(Role.ROLE_USER);
        person.setEnable(false);

//        emailService.sendEmail(new EmailDto(
//                person.getEmail(),
//                "Activation key",
//                "To activate account please follow the link\nhttps://store.bashkirov.space/activate/" + key
//        ));

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

        String key = activationService.generateKey(person.getEmail());
//        Thread.sleep(1000);

        emailService.sendActivationEmail(person.getEmail(), key);
    }

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void runCleanupNotActivatedPersonAccounts() {
        System.out.println("Запустился метод runCleanupNotActivatedPersonAccounts");
        personDetailsService.deleteNotActivatedPersonAccountsMoreThen2HoursAgo();
    }
}
