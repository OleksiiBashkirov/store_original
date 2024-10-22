package bashkirov.store_original.controller;

import bashkirov.store_original.model.Orders;
import bashkirov.store_original.model.Person;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/new")
    public String orderNewPage(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        Person person = personDetails.person();
        Orders order = new Orders();
        order.setName(person.getName());
        order.setLastname(person.getLastname());
        order.setPhone(person.getPhone());
        order.setDeliveryAddress(person.getAddress());
        model.addAttribute("orderNew", order);
        return "order/order-new-page";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("orderNew") Orders orderNew,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "order/order-new-page";
        }
        orderService.createOrder(orderNew);
        return "redirect:/product";
    }

    @GetMapping
    public String getUserOrders(
            Model model
    ) {
        model.addAttribute("ordersHistoryByUser", orderService.getAllByUser());

        return "order/all";
    }

    //ДЗ: зробити сторінку одного замовлення поки кнопок там ніяких не давати,
    // для адміна зробити сторінку всіх замовлень,
    // там можна фільтрувати по статусам (кнопок поки не добавляти і всюда гарні стилі)


}
