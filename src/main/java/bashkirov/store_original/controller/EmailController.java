package bashkirov.store_original.controller;

import bashkirov.store_original.dto.EmailDto;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.service.EmailService;
import bashkirov.store_original.service.PersonDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    private final PersonDetailsService personDetailsService;

    @GetMapping
    public String sendEmailToCustomer(
            @RequestParam("orderId") int orderId,
            @RequestParam("text") String text,
            RedirectAttributes redirectAttributes
    ) {

        Person person = personDetailsService.getPersonByOrderId(orderId);
        String email = person.getEmail();
        emailService.sendEmail(new EmailDto(email, "Магазин BASHKIROV", text));
        redirectAttributes.addAttribute("isSent", true);
        return "redirect:/order/admin/" + orderId;
    }
}
