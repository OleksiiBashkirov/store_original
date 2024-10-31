package bashkirov.store_original.service;

import bashkirov.store_original.config.BotConfig;
import bashkirov.store_original.dto.EmailDto;
import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.model.TelegramCorrespondence;
import bashkirov.store_original.model.TelegramUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TelegramBot extends TelegramLongPollingBot {
    private static final List<String> DEFAULT_BUTTONS = List.of(
            "Акційні пропозиції",
            "Про нас",
            "Пошук",
            "Потрібна допомога",
            "Замовити дзвінок"  // закоментувати
    );

    private final BotConfig botConfig;
    private final ProductService productService;
    private final TelegramUserService telegramUserService;
    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final OrderPhoneCallService orderPhoneCallService;
    private final TelegramCorrespondenceService telegramCorrespondenceService;

    public TelegramBot(
            BotConfig botConfig,
            ProductService productService,
            TelegramUserService telegramUserService,
            JdbcTemplate jdbcTemplate,
            EmailService emailService, OrderPhoneCallService orderPhoneCallService, TelegramCorrespondenceService telegramCorrespondenceService
    ) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.productService = productService;
        this.telegramUserService = telegramUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
        this.orderPhoneCallService = orderPhoneCallService;
        this.telegramCorrespondenceService = telegramCorrespondenceService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();

        if (telegramUserService.getByChatId(chatId).isEmpty()) {
            TelegramUser telegramUser = new TelegramUser();

            telegramUser.setChatId(chatId);
            telegramUser.setUsername(update.getMessage().getChat().getUserName());
            if (update.getMessage().hasContact()) {
                telegramUser.setPhone(update.getMessage().getContact().getPhoneNumber());
            }
            telegramUser.setName(update.getMessage().getChat().getFirstName());
            telegramUser.setLastname(update.getMessage().getChat().getLastName());

            telegramUserService.save(telegramUser);
        }

        if (update.getMessage().hasContact()) {
            telegramUserService.addTelegramUserPhoneNumberByChatId(chatId, update.getMessage().getContact().getPhoneNumber());
            orderPhoneCall(chatId);
        } else if (update.getMessage().hasText()) {
            String message = update.getMessage().getText().trim();
            if (message.equalsIgnoreCase("замовити дзвінок")) {
                orderPhoneCall(chatId);
            } else {
                telegramCorrespondenceService.save(
                        new TelegramCorrespondence(
                                chatId, false, update.getMessage().getText(), LocalDateTime.now()
                        ));
                sendChatAction(chatId, ActionType.TYPING);
                System.out.println("message= " + message);
                textMessageHandler(message, chatId);
            }
        }
    }

    private void textMessageHandler(String message, long chatId) {
        switch (message.trim().toLowerCase()) {
            case "/start", "головне меню" -> onStart(chatId);
            case "акційні пропозиції" -> sendMarkdownMessageWithButtons(
                    chatId,
                    productService.getFiveRandomProductSaleDto(),
                    DEFAULT_BUTTONS
            );
            case "про нас" -> sendMarkdownMessageWithButtons(
                    chatId,
                    productService.getInfoAboutUs(),
                    DEFAULT_BUTTONS
            );
            case "пошук" -> sendMessage(
                    chatId,
                    "Введіть ключове слово для пошуку:"
            );
            case "потрібна допомога" -> sendMarkdownMessageWithButtons(
                    chatId,
                    getHelpInfo(),
                    DEFAULT_BUTTONS
            );
            case "замовити дзвінок" -> orderPhoneCall(
                    chatId
            );
            default -> handleSearchQuery(message, chatId);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити повідомлення в чат " + chatId);
        }
    }

    public void sendMessageToUserByAdmin(long chatId, String message) {
        sendMessageWithButtons(chatId, message, DEFAULT_BUTTONS);
    }

    public void sendMessageToAllUsersByAdmin(String message) {
        List<TelegramUser> all = telegramUserService.getAll();
        for (TelegramUser telegramUser : all) {
            sendMessageToUserByAdmin(telegramUser.getChatId(), message);
            telegramCorrespondenceService.save(new TelegramCorrespondence(
                    telegramUser.getChatId(),
                    true,
                    message,
                    LocalDateTime.now()
            ));
        }
    }

    private void sendMessageWithButtons(long chatId, String message, List<String> buttons) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getButtons(buttons);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити повідомлення з кнопками");
        }
    }

    private void orderPhoneCall(long chatId) {
        TelegramUser telegramUser = telegramUserService.getByChatId(chatId).orElseThrow();

        if (telegramUser.getPhone() != null) {
            saveOrderPhoneCall(telegramUser);
            sendMessageWithButtons(
                    chatId,
                    "Дзвінок замовлено. Вам перетелефонують протягом 2 хвилин",
                    DEFAULT_BUTTONS);
            sendOrderNotificationEmail(telegramUser);
        } else {
            requestPhoneNumber(chatId);
            // ТУТ потрібно ловити номер телефона, який надішлють
            // І зберігати його телеграмЮзеру
        }
    }

    private void saveOrderPhoneCall(TelegramUser telegramUser) {
        orderPhoneCallService.save(telegramUser);
    }

    private void sendOrderNotificationEmail(TelegramUser telegramUser) {
        EmailDto emailDto = new EmailDto(
                "bashkirov.o.u@gmail.com",
                "BashkirovShopBot. Нове замовлення дзвінка.",
                "Замовлено дзвінок від користувача: " + telegramUser.getUsername()
                        + ".\n Телефон: " + telegramUser.getPhone()
                        + ", " + telegramUser.getName() + " " + telegramUser.getLastname()
        );
        emailService.sendEmail(emailDto);
    }

    private void requestPhoneNumber(long chatId) {
        String buttonText = "Надати свій номер телефону ";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Введіть номером телефону у форматі 38ХХХХХХХХХХ, щоб ми могли з Вами зв'язатись:");
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getButtonWithRequiredContact(buttonText));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось попросити телефон");
        }
    }

    private ReplyKeyboardMarkup getButtonWithRequiredContact(String text) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        KeyboardRow keyboardRow = new KeyboardRow();
        KeyboardButton keyboardButton = new KeyboardButton(text);

        keyboardButton.setRequestContact(true);
        keyboardRow.add(keyboardButton);

        List<KeyboardRow> keyboard = Collections.singletonList(keyboardRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }


    private void onStart(long chatId) {
        String message = """
                Вітаємо Вас в BashkirovShopBot!
                Сподіваємось Вам сподобається їм користуватись.
                Не забудьте написати відгук купленого товару!
                Гарних покупок!
                """;

        sendMessageWithButtons(chatId, message, DEFAULT_BUTTONS
        );
    }

    private void sendChatAction(long chatId, ActionType actionType) {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(chatId);
        sendChatAction.setAction(actionType);
        try {
            execute(sendChatAction);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити chatAction");
        }
    }

    private ReplyKeyboardMarkup getButtons(List<String> buttons) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String button : buttons) {
            KeyboardRow row = new KeyboardRow();
            KeyboardButton btn = new KeyboardButton(button);
            row.add(btn);
            keyboard.add(row);
        }
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private void sendMarkdownMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("MarkdownV2");

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити повідомлення в чат " + chatId);
        }
    }

    private void sendMarkdownMessageWithButtons(long chatId, String message, List<String> buttons) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getButtons(buttons);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setParseMode("Markdown");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити повідомлення в чат " + chatId);
        }
    }

    private void handleSearchQuery(String query, long chatId) {
        List<ProductPhotoDto> searchResults = productService.search(query, null, 0, 5);
        if (searchResults.isEmpty()) {
            sendMessage(chatId, "За вашим запитом нічого не знайдено.");
        } else {
            StringBuilder sb = new StringBuilder("🔍 *Результати пошуку*:\n\n");
            for (ProductPhotoDto product : searchResults) {
                sb.append("🛒 *Продукт*: ").append(product.getProduct().getTitle()).append("\n")
                        .append("💲 Ціна: ").append(product.getProduct().getPrice()).append(" грн\n")
                        .append("🔗 [Деталі продукту] ").append("https://store.bashkirov.space/product/")
                        .append(product.getProduct().getId()).append("\n\n");
            }
            sendMarkdownMessageWithButtons(chatId, sb.toString(), DEFAULT_BUTTONS);
        }
    }

    private String getHelpInfo() {
        return """
                 🆘 *Потрібна допомога?* 🆘
                
                            📞 *Контактний номер*: +38 (073) 001-003-1
                            ✉️ *Електронна пошта*: support@bashkirov.shop
                            🌐 *Вебсайт*: [bashkirov.shop](https://store.bashkirov.space/product)
                
                            Ми завжди раді допомогти вам з будь-якими питаннями!
                """;
    }
}
