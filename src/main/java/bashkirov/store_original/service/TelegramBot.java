package bashkirov.store_original.service;

import bashkirov.store_original.config.BotConfig;
import bashkirov.store_original.dto.EmailDto;
import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.model.TelegramCorrespondence;
import bashkirov.store_original.model.TelegramUser;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramBot extends TelegramLongPollingBot {
    private static final List<String> DEFAULT_BUTTONS = List.of(
            "Акційні пропозиції",
            "Про нас",
            "Пошук",
            "Потрібна допомога",
            "Замовити дзвінок"
    );

    private final BotConfig botConfig;
    private final ProductService productService;
    private final TelegramUserService telegramUserService;
    private final EmailService emailService;
    private final OrderPhoneCallService orderPhoneCallService;
    private final TelegramCorrespondenceService telegramCorrespondenceService;
    private final ChatGptService chatGptService;
    private final HttpMessageConverters messageConverters;

    public TelegramBot(
            BotConfig botConfig,
            ProductService productService,
            TelegramUserService telegramUserService,
            EmailService emailService,
            OrderPhoneCallService orderPhoneCallService,
            TelegramCorrespondenceService telegramCorrespondenceService,
            ChatGptService chatGptService,
            HttpMessageConverters messageConverters) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.productService = productService;
        this.telegramUserService = telegramUserService;
        this.emailService = emailService;
        this.orderPhoneCallService = orderPhoneCallService;
        this.telegramCorrespondenceService = telegramCorrespondenceService;
        this.chatGptService = chatGptService;
        this.messageConverters = messageConverters;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();
        // если юзера нет, создаем
        if (telegramUserService.getByChatId(chatId).isEmpty()) {
            TelegramUser telegramUser = new TelegramUser();

            telegramUser.setChatId(chatId);
            telegramUser.setUsername(update.getMessage().getChat().getUserName());
            //якщо має контакт
            if (update.getMessage().hasContact()) {
                telegramUser.setPhone(update.getMessage().getContact().getPhoneNumber());
            }
            telegramUser.setName(update.getMessage().getChat().getFirstName());
            telegramUser.setLastname(update.getMessage().getChat().getLastName());

            telegramUserService.save(telegramUser);
        }
        // если у юзера есть контакт, сохраняем в БД
        if (update.getMessage().hasContact()) {
            telegramUserService.addTelegramUserPhoneNumberByChatId(chatId, update.getMessage().getContact().getPhoneNumber());
            orderPhoneCall(chatId);

            // если есть текст в сообщение, обрабатываем текст
        } else if (update.getMessage().hasText()) {
            String message = update.getMessage().getText().trim();
            if (message.startsWith("?")) {
                CompletableFuture.runAsync(
//                        ()-> sendChatAction(chatId, ActionType.TYPING),
                                () -> sendDelayedResponse(chatId, message))
                        .thenRunAsync(
                                () -> sendMessage(chatId, chatGptService.chatGpt(message, ""))
                        );
                saveMessageToCorrespondence(chatId, true, message);
            } else if (message.equalsIgnoreCase("замовити дзвінок")) {
                orderPhoneCall(chatId);
            } else {
                //зберігаємо переписку
                saveMessageToCorrespondence(chatId, false, update.getMessage().getText());
                sendChatAction(chatId, ActionType.TYPING);
                System.out.println("message= " + message);
                //обробляємо повідомлення в пошуку
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
            saveMessageToCorrespondence(telegramUser.getChatId(), true, message);
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
            System.out.println("Не вдалося відправити повідомлення користувачу " + chatId + ". Ймовірно, користувач видалив бота.");
            telegramUserService.deleteByChatId(chatId);
        }
    }

    private void orderPhoneCall(long chatId) {
        TelegramUser telegramUser = telegramUserService.getByChatId(chatId).orElseThrow();

        if (telegramUser.getPhone() != null) {
            saveOrderPhoneCall(telegramUser);
            String message = "\uD83D\uDCDE Дзвінок замовлено. Вам перетелефонують протягом *2 хвилин*.";
            sendMarkdownMessageWithButtons(chatId, message, DEFAULT_BUTTONS);
            saveMessageToCorrespondence(chatId, true, message);
            sendOrderNotificationEmail(telegramUser);
        } else {
            requestPhoneNumber(chatId);
        }
    }

    private void saveMessageToCorrespondence(long chatId, boolean isAdmin, String message) {
        telegramCorrespondenceService.save(new TelegramCorrespondence(chatId, isAdmin, message, LocalDateTime.now()));
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
        String buttonText = "Поділитись контактом";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("\uD83D\uDCF1 Введіть номер телефону у форматі `38ХХХХХХХХХХ`, щоб ми могли з Вами зв'язатись:");
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getButtonWithRequiredContact(buttonText));
        try {
            execute(sendMessage);
            saveMessageToCorrespondence(chatId, true, sendMessage.getText());
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось попросити телефон");
        }
    }

    private void processPhoneNumber(long chatId, String phoneNumber) {
        if (isValidPhoneNumber(phoneNumber)){
            telegramUserService.addTelegramUserPhoneNumberByChatId(chatId, phoneNumber);
            String message = "Ваш номер телефону збережено.";
            sendMessage(chatId, message);
            saveMessageToCorrespondence(chatId, true, message);
            orderPhoneCall(chatId);
        } else {
            String message = "Номер телефону некоректний. Будь ласка, введіть правильний номер у форматі `+КодКраїниНомерТелефону`.";
            sendMessage(chatId, message);
            saveMessageToCorrespondence(chatId, true, message);
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
                🎉 *Вітаємо Вас в BashkirovShopBot!* 🎉
                Сподіваємось, що вам сподобається користуватись нашим ботом.
                Не забудьте написати відгук про куплений товар! 🛒
                *Гарних покупок!* 🛍️
                """;

        sendMarkdownMessageWithButtons(chatId, message, DEFAULT_BUTTONS);
        saveMessageToCorrespondence(chatId, true, message);
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

    private void sendMarkdownMessageWithButtons(long chatId, String message, List<String> buttons) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getButtons(buttons);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setParseMode("MarkdownV2");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Не вдалось відправити повідомлення в чат " + chatId);
        }
    }

    private void handleSearchQuery(String query, long chatId) {
        List<ProductPhotoDto> searchResults = productService.search(query, null, 0, 5);
        if (searchResults.isEmpty()) {
            String message = "За вашим запитом нічого не знайдено.";
            sendMessage(chatId, message);
            saveMessageToCorrespondence(chatId, true, message);
        } else {
            StringBuilder sb = new StringBuilder("🔍 *Результати пошуку*:\n\n");
            for (ProductPhotoDto product : searchResults) {
                sb.append("🛒 *Продукт*: ").append(product.getProduct().getTitle()).append("\n")
                        .append("💲 Ціна: ").append(product.getProduct().getPrice()).append(" грн\n")
                        .append("🔗 [Деталі продукту] ").append("https://store.bashkirov.space/product/")
                        .append(product.getProduct().getId()).append("\n\n");
            }
            sendMarkdownMessageWithButtons(chatId, sb.toString(), DEFAULT_BUTTONS);
            saveMessageToCorrespondence(chatId, true, sb.toString());
        }
    }

    private String getHelpInfo() {
        return """
                 🆘 *Потрібна допомога?* 🆘
                 У нас є віртуальний помічник!
                 Якщо хочете задати йому питання, почніть речення зі знаку питання `?`,
                 наприклад:"?Як тебе звати".
                
                 Або зв'яжіться з нами:
                            📞 *Контактний номер*: +38 (073) 001-003-1
                            ✉️ *Електронна пошта*: support@bashkirov.shop
                            🌐 *Вебсайт*: [bashkirov.shop](https://store.bashkirov.space/product)
                
                 Ми завжди раді допомогти вам з будь-якими питаннями!
                """;
    }

    private void sendDelayedResponse(long chatId, String message) {
        try {
            TimeUnit.SECONDS.sleep(3);
            String intermediateAnswer = "Ваш запит обробляється. Це може зайняти кілька секунд...";
            sendMessage(chatId, intermediateAnswer);
            saveMessageToCorrespondence(chatId, true, intermediateAnswer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error in delayed responce: " + e.getMessage());
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String phoneRegex = "^\\+?\\d{7,15}$";
        return phoneNumber.matches(phoneRegex);
    }
}
