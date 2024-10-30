package bashkirov.store_original.controller;

import bashkirov.store_original.dto.PaymentDto;
import bashkirov.store_original.service.PaymentService;
import bashkirov.store_original.validation.PaymentValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentValidator paymentValidator;

    @GetMapping("/{orderId}")
    public String paymentPage(
            @PathVariable("orderId") int orderId,
            Model model,
            @ModelAttribute("paymentDto") PaymentDto paymentDto
    ) {
        model.addAttribute("orderId", orderId);
        return "payment/payment-page";
    }

    @PostMapping("/{orderId}")
    public String save(
            @PathVariable("orderId") int orderId,
            @Valid @ModelAttribute("paymentDto") PaymentDto paymentDto,
            BindingResult bindingResult

    ) {
        paymentValidator.validate(paymentDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return "payment/payment-page";
        }
        paymentService.makePayment(orderId);
        return "payment/payment-success-page";
    }



}
