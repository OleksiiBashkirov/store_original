package bashkirov.store_original.service;

import bashkirov.store_original.config.BotConfig;
import bashkirov.store_original.dto.ProductPhotoDto;
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

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBot extends TelegramLongPollingBot {
    private static final List<String> DEFAULT_BUTTONS = List.of(
            "Акційні пропозиції",
            "Про нас",
            "Пошук",
            "Потрібна допомога"
//            "Замовити дзвінок"
    );

    private final BotConfig botConfig;
    private final ProductService productService;
    private final TelegramUserService telegramUserService;

    public TelegramBot(BotConfig botConfig, ProductService productService, TelegramUserService telegramUserService) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.productService = productService;
        this.telegramUserService = telegramUserService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();

//        if (telegramUserService.getByChatId(chatId).isEmpty()) {
//            TelegramUser telegramUser = new TelegramUser();
//
//            telegramUser.setChatId(chatId);
//            telegramUser.setUsername(update.getMessage().getChat().getUserName());
//            telegramUser.setPhone(update.getMessage().getContact().getPhoneNumber());
//            telegramUser.setName(update.getMessage().getChat().getFirstName());
//            telegramUser.setName(update.getMessage().getChat().getLastName());
//
//            telegramUserService.save(telegramUser);
//        }


        if (update.getMessage().hasText()) {
            sendChatAction(chatId, ActionType.TYPING);
            String message = update.getMessage().getText();
            System.out.println("message= " + message);
            textMessageHandler(message, chatId);
        }
    }

    /*
        private void textMessageHandler(String message, long chatId) {
            switch (message) {
                case "/start", "Головне меню" -> onStart(chatId);
                case "Акційні пропозиції" -> sendMarkdownMessage(
                        chatId,
                        productService.getFirstFiveRandomProductSaleDto()
    //                    DEFAULT_BUTTONS
                );
            }
        }
      */
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
//            case "замовити дзвінок" -> orderPhoneCall(
//                    chatId
//            );
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
/*
    private void orderPhoneCall(long chatId) {
        TelegramUser byChatId = telegramUserService.getByChatId(chatId).get();
        if (byChatId.getPhone() != null) {
            // TODO зробити таблицю із заказами для дзвінків ->
            //  адмін має сторінку з цими дзвінками ->
            //  на пошту має приходити, що дзвінок замовлено

            sendMessageWithButtons(
                    chatId,
                    "Очікуйте дзвінок протягом 2 хвилин",
                    DEFAULT_BUTTONS);
            return;
        }

        String buttonText = "Введіть номер телефону";
        SendMessage sendMessage = new SendMessage();
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
*/
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

    //акційні товари, пошук товарів, потрібна допомога

}
