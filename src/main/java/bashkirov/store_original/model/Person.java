package bashkirov.store_original.model;

import bashkirov.store_original.enumeration.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person {
    private int id;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 2, max = 32, message = "Size should be at least 2 and not longer 32 characters")
    private String name;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 2, max = 32, message = "Size should be at least 2 and not longer 32 characters")
    private String lastname;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 7, max = 128, message = "Address should be like `01234, Country, other address parts")
    @Pattern(regexp = "^\\d{5}, [a-zA-Z]{2,20}, [a-zA-Z0-9 ,.\\-']+$")
    private String address;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 10, max = 13, message = "Phone should have at least 10 and not longer 13 characters")
    @Pattern(regexp = "^\\+?\\d{7,12}$", message = "Phone should be like `+380501234567` or `0459512345`")
    private String phone;

    @Email
    @Size(min = 2, max = 64, message = "Size should be at least 2 and not longer 64 characters")
    private String email;

    @NotBlank(message = "Field cannot be empty")
    @Size(min = 2, max = 32, message = "Size should be at least 2 and not longer 32 characters")
    private String username;

    @NotBlank(message = "Field cannot be empty")
    private String password;

    private Role role;

    private boolean isEnable;
}
