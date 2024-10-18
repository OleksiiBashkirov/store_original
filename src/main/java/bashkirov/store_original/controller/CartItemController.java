package bashkirov.store_original.controller;

import bashkirov.store_original.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartItemController {
    private final CartItemService cartItemService;

    @PostMapping
    public String add(
            @RequestParam("productId") int productId
    ) {
        cartItemService.add(productId);
        return "redirect:/product/" + productId;
    }

//    @PostMapping("/add/{id}")
//    public String addAndRedirectToShoppingCart(
//            @PathVariable("id") int id
//    ) {
//        cartItemService.add(id);
//        return "shopping-cart/shoppingCart-page";
//    }

    @GetMapping
    public String showShoppingCart(
            Model model
    ) {
        model.addAttribute("cartItemsListNotTakenReturnCartItemDto",
                cartItemService.getAllNotTakenReturnCartItemDto());
        return "shopping-cart/shoppingCart-page";
    }

    @PutMapping("/edit")
    public String updateCartItemCount(
            @RequestParam("cartItemId") int cartItemId,
            @RequestParam("quantityNew") int quantityNew
    ) {
        cartItemService.update(cartItemId, quantityNew);
        return "redirect:/cart";
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable("id") int id
    ) {
        cartItemService.delete(id);
        return "redirect:/cart";
    }
}
