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
    public String addCartItem(
            @RequestParam("productId") int productId
    ) {
        cartItemService.addCartItemByProductId(productId);
        return "redirect:/product/" + productId;
    }

    @GetMapping
    public String showShoppingCart(
            Model model
    ) {
        model.addAttribute("cartItemDtos", cartItemService.getAllCartItemDtosInShoppingCart());
        model.addAttribute("totalSum", cartItemService.getTotalPriceOffAllCartItemsInShoppingCart());
        return "shopping-cart/shoppingCart-page";
    }

    @PutMapping("/edit")
    public String updateCartItemQuantity(
            @RequestParam("cartItemId") int cartItemId,
            @RequestParam("quantityNew") int quantityNew
    ) {
        cartItemService.updateCartItemQuantity(cartItemId, quantityNew);
        return "redirect:/cart";
    }

    @DeleteMapping("/{cartItemId}")     //!!!!
    public String deleteCartItem(
            @PathVariable("cartItemId") int cartItemId
    ) {
        cartItemService.deleteCartItemById(cartItemId);
        return "redirect:/cart";
    }
}
