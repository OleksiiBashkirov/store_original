package bashkirov.store_original.controller;

import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.PromoCode;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/promocode")
@RequiredArgsConstructor
public class PromoCodeController {
    private final PromoCodeService promoCodeService;

//    @GetMapping("/{id}")
//    public String showPromoCodePage(
//            @PathVariable("id") int promoCodeId,
//            Model model
//    ) {
//        model.addAttribute("promoCode", promoCodeService.getPromoCodeById(promoCodeId));
//        return "promocode/promocode-page";
//    }

    @GetMapping
    public String showAllPromoCodePage(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        boolean isAdmin = personDetails != null
                && personDetails.person().getRole().equals(Role.ROLE_ADMIN);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("promoCodes", promoCodeService.getAllPromoCode());
        model.addAttribute("promoCodesValid", promoCodeService.getAllValidPromoCodes());

        return "promocode/promocodes-page";

    }

    @GetMapping("/new")
    public String promoCodeNewPage(
            @ModelAttribute("promoCodeNew") PromoCode promoCode
    ) {
        return "promocode/promocode-new-page";
    }

    @PostMapping("/new")
    public String savePromoCode(
            @ModelAttribute("promoCodeNew") PromoCode promoCode
    ) {
        promoCodeService.savePromoCode(promoCode);
        return "redirect:/promocode";

    }
}
