package bashkirov.store_original.service;

import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.security.PersonDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Person> optionalPerson = getOptionalPersonByUsername(username);
        if (optionalPerson.isEmpty()) {
            throw new UsernameNotFoundException("Failed to find user with username= " + username);
        }

        return new PersonDetails(optionalPerson.get());
    }

    public Optional<Person> getOptionalPersonByUsername(String username) {
        return jdbcTemplate.query(
                "select * from person where username = ?",
                new Object[]{username},
                getPersonRowMapper()

        ).stream().findAny();
    }

    public Optional<Person> getOptionalPersonByEmail(String email) {
        return jdbcTemplate.query(
                "select * from person where email = ?",
                new Object[]{email},
                getPersonRowMapper()

        ).stream().findAny();
    }

    public List<Person> getAllAdmins() {
        return jdbcTemplate.query(
                "select * from person where role = ?",
                new Object[]{Role.ROLE_ADMIN.toString()},
                new BeanPropertyRowMapper<>(Person.class)
        );
    }

    public List<Person> getAllManagers() {
        return jdbcTemplate.query(
                "select * from person where role = ?",
                new Object[]{Role.ROLE_MANAGER.toString()},
                new BeanPropertyRowMapper<>(Person.class)
        );
    }

    public List<Person> getAllUsers() {
        return jdbcTemplate.query(
                "select * from person where role = ?",
                new Object[]{Role.ROLE_USER.toString()},
                new BeanPropertyRowMapper<>(Person.class)
        );
    }

    private static RowMapper<Person> getPersonRowMapper() {
        return (rs, rowNum) -> {
            Person person = new Person();
            person.setId(rs.getInt("id"));
            person.setName(rs.getString("name"));
            person.setLastname(rs.getString("lastname"));
            person.setAddress(rs.getString("address"));
            person.setPhone(rs.getString("phone"));
            person.setEmail(rs.getString("email"));
            person.setUsername(rs.getString("username"));
            person.setPassword(rs.getString("password"));
            person.setRole(Role.valueOf(rs.getString("role")));
            person.setEnable(rs.getBoolean("is_enable"));
            return person;
        };
    }

    public Person getPersonByOrderId(int orderId) {
        return jdbcTemplate.query(
                "select * from person p join orders o on p.id = o.person_id where o.id = ?",
                new Object[]{orderId},
                getPersonRowMapper()
        ).stream().findAny().orElseThrow(
                () -> new NoSuchElementException("Failed to find person by order id=" + orderId));
    }

    public void addAdmin(int personId) {
        jdbcTemplate.update(
                "update person set role = 'ROLE_ADMIN' where id = ?",
                personId
        );
    }

    public void removeAdmin(int personId) {
        jdbcTemplate.update(
                "update person set role = 'ROLE_USER' where id = ?",
                personId
        );
    }

    public Person getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            PersonDetails userDetails = (PersonDetails) authentication.getPrincipal();
            return userDetails.person();
        }
        return null;
    }

    public void update(Person person) {
        jdbcTemplate.update(
                "update person set name = ?, lastname = ?, address = ?, phone = ? where id = ?",
                person.getName(),
                person.getLastname(),
                person.getAddress(),
                person.getPhone(),
                getCurrentUser().getId()
        );
    }
}
