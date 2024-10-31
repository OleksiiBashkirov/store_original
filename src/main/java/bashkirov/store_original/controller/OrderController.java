package bashkirov.store_original.controller;

import bashkirov.store_original.dto.OrderCartItemsDto;
import bashkirov.store_original.enumeration.OrdersStatus;
import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Orders;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.OrderService;
import bashkirov.store_original.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/new")
    public String orderNewPage(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        Orders order = orderService.getForOrderPersonDetails();
        model.addAttribute("orderNew", order);

        return "order/order-new-page";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("orderNew") Orders orderNew,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "order/order-new-page";
        }
        orderService.createOrder(orderNew);
        OrderCartItemsDto lastUserOrderWithCartItemsDto = orderService.getLastUserOrderWithCartItemsDto();

        return "redirect:/order/" + lastUserOrderWithCartItemsDto.getOrder().getId();
    }

    @GetMapping
    public String getUserOrders(
            Model model
    ) {
        model.addAttribute("ordersHistoryByUser", orderService.getAllByUser());

        return "order/user-orders";
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

    @GetMapping("/all-orders")
    public String showAllOrders(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false, name = "status") OrdersStatus status,
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        if (personDetails.person().getRole().equals(Role.ROLE_ADMIN)) {
            model.addAttribute("admin", true);
        }

        List<Orders> ordersList;

        if (status != null) {
            ordersList = orderService.getAllByStatus(status);
        } else if (sortBy.equals("id")) {
            ordersList = order.equals("asc") ? orderService.getAllSortedByOrderIdAsc()
                    : orderService.getAll();
        } else if (sortBy.equals("created_at")) {
            ordersList = order.equals("asc") ? orderService.getAllSortedByCreatedAtAsc()
                    : orderService.getAllSortedByCreatedAtDesc();
        } else {
            ordersList = orderService.getAll();
        }

        model.addAttribute("ordersList", ordersList);
        model.addAttribute("statusList", OrdersStatus.values());
        model.addAttribute("selectedStatus", status);
        return "order/all-orders-page";
    }

//***********************************************************************************
    // + треба для адміна зробити теж сторінку замовлення
    // + але адмін на ній зможе сам вибирати любий статус який йому до вподоби
//***********************************************************************************
    // а також буде кнопка видалити замовлення,
    // + також поле через яке ми зможем відправити лист на пошту даній людині
//***********************************************************************************

    @GetMapping("/admin/{orderId}")
    public String showAdminOrderPage(
            @PathVariable("orderId") int orderId,
            @RequestParam(required = false, name = "isSent") Boolean isSent,
            Model model
    ) {
        model.addAttribute("orderByIdWithCartItemsList", orderService.getWithCartItemsDtoById(orderId));
        model.addAttribute("statusList", OrdersStatus.values());
        model.addAttribute("isSent", isSent != null);
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
