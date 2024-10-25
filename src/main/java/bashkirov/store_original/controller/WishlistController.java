package bashkirov.store_original.controller;

import bashkirov.store_original.dto.WishlistDto;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public String showWishlistPage(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        Person person = personDetails.person();
        List<WishlistDto> wishlistDtoList = wishlistService.getWishlistListDtoByPersonId(person.getId());
        model.addAttribute("wishlistDtoList", wishlistDtoList);
        return "wishlist/wishlist-page";
    }

    @PostMapping("/add")
    public String addToWishlist(
            @RequestParam("productId") int productId
    ) {
        wishlistService.addProductToWishList(productId);
        return "redirect:/product";
    }

    @DeleteMapping("/remove")
    public String removeFromWishlist(
            @RequestParam("productId") int productId
    ) {
        wishlistService.removeProductFromWishlist(productId);
        return "redirect:/wishlist";
    }
}
