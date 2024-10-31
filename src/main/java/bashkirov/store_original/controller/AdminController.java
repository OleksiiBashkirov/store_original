package bashkirov.store_original.controller;

import bashkirov.store_original.model.Person;
import bashkirov.store_original.service.OrderPhoneCallService;
import bashkirov.store_original.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PersonDetailsService personDetailsService;
    private final OrderPhoneCallService orderPhoneCallService;

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
        return "admin/list-of-all-users";

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

    @GetMapping("/order-phone-calls")
    public String showOrderPhoneCallsPage(
            Model model
    ) {
        model.addAttribute("orderPhoneCalls", orderPhoneCallService.getAllPhoneOrders());
        return "admin/order-phone-call-page";
    }

    @PutMapping("/order-phone-calls")
    public String updatePhoneCall(
            @RequestParam("id") int id,
            Model model
    ) {
        model.addAttribute("phoneCallById", orderPhoneCallService.getById(id));
        orderPhoneCallService.update(id, true);
        return "redirect:/admin/order-phone-calls";
    }

    @DeleteMapping("/order-phone-calls")
    public String deletePhoneCall(
            @RequestParam("id") int id
    ) {
        orderPhoneCallService.deleteById(id);
        return "redirect:/admin/order-phone-calls";
    }
}
