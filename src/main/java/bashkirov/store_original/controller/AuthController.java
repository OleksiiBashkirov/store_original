package bashkirov.store_original.controller;

import bashkirov.store_original.model.Person;
import bashkirov.store_original.service.RegistrationService;
import bashkirov.store_original.validation.PersonValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegistrationService registrationService;
    private final PersonValidator personValidator;

    @GetMapping("/registration")
    public String registrationPage(
            @ModelAttribute("personNew") Person personNew
    ) {
        return "registration-page";
    }

    @PostMapping("/registration")
    public String registration(
            @Valid @ModelAttribute("personNew") Person personNew,
            BindingResult bindingResult
    ) {
        personValidator.validate(personNew, bindingResult);
        if (bindingResult.hasErrors()) {
            return "registration-page";
        }
        registrationService.register(personNew);
        return "redirect:/auth/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "enter-page";
    }

    @GetMapping("/logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login";
    }
}
