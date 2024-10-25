package bashkirov.store_original.controller;

import bashkirov.store_original.model.Person;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.PersonDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {
    private final PersonDetailsService personDetailsService;

    @GetMapping("/profile")
    public String showProfile(
            @AuthenticationPrincipal PersonDetails personDetails,
            Model model
    ) {
        model.addAttribute("person", personDetails.person());
        return "person/person-profile";
    }

    @PutMapping("/edit")
    public String editProfile(
            @Valid @ModelAttribute("person") Person person,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "person/person-profile";
        }

        personDetailsService.update(person);

        return "redirect:/person/profile";
    }
}
