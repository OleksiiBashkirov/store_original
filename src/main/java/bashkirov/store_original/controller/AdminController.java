package bashkirov.store_original.controller;

import bashkirov.store_original.model.Person;
import bashkirov.store_original.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PersonDetailsService personDetailsService;

    @GetMapping
    public String adminPage(
            @ModelAttribute("person") Person person,
            Model model
    ) {
        List<Person> adminsList = personDetailsService.getAllAdmins();
        List<Person> managersList = personDetailsService.getAllManagers();

        model.addAttribute("adminsList", adminsList);
        model.addAttribute("managersList", managersList);
        return "admin/admin-page";
    }

    @GetMapping("/users")
    public String showAllUsers(
            @ModelAttribute("person") Person person,
            Model model
    ) {
        List<Person> usersList = personDetailsService.getAllUsers();
        model.addAttribute("usersList", usersList);
        return "admin/users-list-page";

    }

    @PatchMapping("/add")
    public String addAdmin(
            @RequestParam("personId") int personId
    ) {
        personDetailsService.addAdmin(personId);
        return "redirect:/admin";
    }

    @PatchMapping("/remove")
    public String removeAdmin(
            @RequestParam("personId") int personId
    ) {
        personDetailsService.removeAdmin(personId);
        return "redirect:/admin/users";
    }
}
