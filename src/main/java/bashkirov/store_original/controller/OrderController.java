package bashkirov.store_original.controller;

import bashkirov.store_original.dto.OrderCartItemsDto;
import bashkirov.store_original.enumeration.OrdersStatus;
import bashkirov.store_original.enumeration.Role;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/{orderId}")
    public String getById(
            @PathVariable("orderId") int orderId,
            Model model
    ) {

        OrderCartItemsDto cartItemsDtoById = orderService.getWithCartItemsDtoById(orderId);
        model.addAttribute("isPending", cartItemsDtoById.getOrder()
                .getOrdersStatus().equals(OrdersStatus.PENDING_PAYMENT));
        model.addAttribute("orderByIdWithCartItemsList", cartItemsDtoById);

        return "order/order-page";
    }

    //ДЗ: зробити сторінку одного замовлення поки кнопок там ніяких не давати,
    // для адміна зробити сторінку всіх замовлень,
    // там можна фільтрувати по статусам (кнопок поки не добавляти і всюда гарні стилі)

    @GetMapping("/all-orders")
    public String showAllOrders(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        if (personDetails.person().getRole().equals(Role.ROLE_ADMIN)) {
            model.addAttribute("admin", true);
        } else {
            model.addAttribute("admin", false);
        }
        model.addAttribute("ordersAll", orderService.getAll());
        return "order/orders-page";
    }
    //коли користувач загодить на своє замовлення і воно не оплачене має бути кнопка оплатити замовлення
    // далі ми попадаєм на сторінку оплати де користувач вводить валідні дані карточки
    // і нажимає знов оплатити після чого статус міняється на оплачений
//*********************************************************************************************************

    //це в нас готова сторінка для юзера
    // тепер треба для адміна зробити теж сторінку замовлення
    // але адмін на ній зможе сам вибирати любий статус який йому до вподоби

    // а також буде кнопка видалити замовлення,
    // також якщо встигнем буде поле
    // через яке ми зможем відправити лист на пошту даній людині
    @GetMapping("/admin/{orderId}")
    public String showAdminOrderPage(
            @PathVariable("orderId") int orderId,
            Model model
    ) {
        model.addAttribute("orderByIdWithCartItemsList", orderService.getWithCartItemsDtoById(orderId));
        model.addAttribute("statusList", OrdersStatus.values());
        return "order/order-admin-page";
    }

    @PutMapping("/admin/{orderId}")
    public String changeStatus(
            @RequestParam("orderStatus") OrdersStatus orderStatus,
            @PathVariable("orderId") int orderId
    ) {
        orderService.updateOrderStatus(orderId, orderStatus);
        return "redirect:/order/admin/" + orderId;
    }


}
