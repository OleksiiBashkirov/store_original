package bashkirov.store_original.service;

import bashkirov.store_original.enumeration.OrdersStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderService orderService;

    public void makePayment(int orderId) {
        orderService.updateOrderStatus(orderId, OrdersStatus.PAID);
    }
}
