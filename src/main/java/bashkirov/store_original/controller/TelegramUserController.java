package bashkirov.store_original.controller;

import bashkirov.store_original.model.TelegramCorrespondence;
import bashkirov.store_original.model.TelegramUser;
import bashkirov.store_original.service.TelegramBot;
import bashkirov.store_original.service.TelegramCorrespondenceService;
import bashkirov.store_original.service.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramUserController {
    private final TelegramUserService telegramUserService;
    private final TelegramBot telegramBot;
    private final TelegramCorrespondenceService telegramCorrespondenceService;


    @GetMapping
    public String getAll(
            Model model
    ) {
        model.addAttribute("telegramUserList", telegramUserService.getAll());
        return "telegram/telegram-user-all-page";
    }

    // (+) Є сторінка юзерів всіх,
    // (+) ще має бути сторінка одного юзера ,
    // (+) на сторінці одного юзера має бути історія чату
    // (+) а також можливість відправити йому повідомлення,
    // ... а на сторінці всіх юзерів просто повинна бути можливість відправити повідомлення всім

    @PostMapping("/send-message-to-user")
    public String sendMessageToUser(
            @RequestParam("message") String message,
            @RequestParam("chatId") long chatId
    ) {
        telegramBot.sendMessageToUserByAdmin(chatId, message);
        telegramCorrespondenceService.save(new TelegramCorrespondence(
                        chatId,
                        true,
                        message,
                        LocalDateTime.now()
                )
        );
        return "redirect:/telegram/" + chatId;
    }

    @PostMapping("/send-message-to-all-users")
    public String sendMessageToAllUsers(
            @RequestParam("message") String message,
            Model model
    ) {
        telegramBot.sendMessageToAllUsersByAdmin(message);
        return "redirect:/telegram";
    }

    @GetMapping("/{chatId}")
    public String showTelegramUserPage(
            @PathVariable("chatId") long chatId,
            Model model
    ) {
        TelegramUser telegramUserByChatId = telegramUserService.getByChatId(chatId).get();
        List<TelegramCorrespondence> allCorrespondenceByChatId = telegramCorrespondenceService.getAllCorrespondenceByChatId(chatId);

        model.addAttribute("telegramUserByChatId", telegramUserByChatId);
        model.addAttribute("allCorrespondenceByChatId", allCorrespondenceByChatId);

        return "telegram/telegram-user-page";
    }




}
