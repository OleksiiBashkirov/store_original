package bashkirov.store_original.validation;

import bashkirov.store_original.model.Person;
import bashkirov.store_original.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PersonValidator implements Validator {
    private final PersonDetailsService personDetailsService;

    @Override
    public boolean supports(Class<?> clazz) {
        return Objects.equals(clazz, Person.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Person person = (Person) target;
        Optional<Person> optionalPerson = personDetailsService.getOptionalPersonByUsername(person.getUsername());
        if (optionalPerson.isPresent()) {
            Person existedPerson = optionalPerson.get();
            if (person.getId() == 0 || person.getId() != existedPerson.getId()) {
                errors.rejectValue(
                        "username",
                        "",
                        String.format("Person with username=%s already exists", person.getUsername())
                );
            }
        }
        optionalPerson = personDetailsService.getOptionalPersonByEmail(person.getEmail());
        if (optionalPerson.isPresent()) {
            Person existedPerson = optionalPerson.get();
            if (person.getId() == 0 || person.getId() != existedPerson.getId()) {
                errors.rejectValue(
                        "email",
                        "",
                        String.format("Person with email=%s already exists", person.getEmail())
                );
            }
        }
    }
}
