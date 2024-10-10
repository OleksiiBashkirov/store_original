package bashkirov.store_original.controller;

import bashkirov.store_original.exception.InvalidActivationKeyException;
import bashkirov.store_original.service.ActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/activate")
@RequiredArgsConstructor
public class ActivationController {
    private final ActivationService activationService;

    @GetMapping("/{key}")
    public String activate(
            @PathVariable("key") String key
    ) {
        try {
            activationService.activate(key);
            return "activation-page-successfully";
        } catch (InvalidActivationKeyException ex) {
            return "activation-page-failure";
        }
    }
}
