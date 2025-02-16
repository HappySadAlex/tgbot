package io.happysadalex.tgbot.service;

import io.happysadalex.tgbot.config.BotConfig;
import io.happysadalex.tgbot.model.UserModel;
import io.happysadalex.tgbot.repository.UserRepository;
import io.happysadalex.tgbot.util.DateValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;


@Slf4j
@Component
public class MainBotService implements LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    private final static String FIO_REQUEST = "Для продолжения пожалуйста пришлите ваши ФИО полностью (через пробел)";
    private final static String THANK_POLICY = "Спасибо за подтвержение!";
    private final static String THANK_SIMPLE = "Спасибо за информацию!";
    private final static String THANK_FOR_ALL_DATA = "Спасибо! Все данные собраны. Скоро вы получите заполненную форму!";
    private final static String GET_BIRTHDATE = "Пожалуйста введите вашу дату рождения в формате ЧИСЛО.МЕСЯЦ.ГОД цифрами (например 25.01.2000 - 25 января 2000 года)";
    private final static String WRONG_FORMAT = "Введенная дата имеет не верный формат!\n" + GET_BIRTHDATE;
    private final static String PHOTO_REQUEST = "Пожалуйста пришлите вашу фотографию";
    private final static String WHAT = "Неизвестная команда :(";

    private boolean isWaitingDate = false;

    private final BotConfig botConfig;

    private final TelegramClient telegramClient;
    private final FileService fileService;
    private final UserRepository userRepo;

    public MainBotService(BotConfig botConfig, FileService fileService, UserRepository userRepo) {
        this.fileService = fileService;
        this.botConfig = botConfig;
        this.userRepo = userRepo;
        telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public void consume(Update update) {
        if(update.hasCallbackQuery()){
            callbackHandler(update.getCallbackQuery());
        }
        else if(update.hasMessage()){
            messageHandler(update.getMessage());
        }
    }

    // обработка колл-бэков
    public void callbackHandler(CallbackQuery callback){
        Long chatId = callback.getFrom().getId();
        Long userId = callback.getFrom().getId();
        UserModel userModel = userRepo.findById(userId).orElseThrow();
        switch (callback.getData()){
            case "policy_agree":
                userRepo.save(userModel.toBuilder().policyAgreement(true).build());
                sendSimpleMessage(chatId, THANK_POLICY);
                sendSimpleMessage(chatId, FIO_REQUEST);
                break;
            case "male", "female":
                userRepo.save(userModel.toBuilder()
                        .sex(callback.getData().equals("male") ? "Мужчина" : "Женщина")
                        .build());
                sendSimpleMessage(chatId, THANK_SIMPLE);
                sendSimpleMessage(chatId, PHOTO_REQUEST);
                break;
        }
    }

    // прием сообщений (либо текстовые, либо фото)
    public void messageHandler(Message message){
        if (message.hasText()) {
            mainMessageHandler(message);
        } else {
            try {
                photoMessageHandler(message);
            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // главный обработчик - обработка текстовых сообщений, включая команды "/start" и подобные если появятся
    public void mainMessageHandler(Message message){
        String messageText = message.getText();
        log.info("Message from {} : {}", message.getFrom().getFirstName(), messageText);

        long chatId = message.getChatId();
        long userId = message.getFrom().getId(); // user id
        String name = message.getChat().getFirstName();
        String[] messageList = messageText.split(" ");

        if(messageText.startsWith("/start")) {
            String sourceUrl = startMessageHandler(messageText); // 1) source url - if exists url, else empty string
            startCommandHandler(chatId, name);
            sendMessageToGetPolicyAgree(chatId);
            // save user source url to db
            userRepo.save(UserModel.builder()
                    .id(userId)
                    .sourceUrl(sourceUrl)
                    .build());
        }
        else if(messageList.length == 3 || messageList.length == 2){
            String firstName = messageList[1];
            String secondName = messageList[0];
            String surname = "";
            if(messageList.length == 3){ surname = messageList[2];}
            // save user FIO to db
            UserModel user = userRepo.findById(userId).orElseThrow();
            userRepo.save(user.toBuilder()
                    .firstName(firstName)
                    .secondName(secondName)
                    .surname(surname)
                    .build());
            sendSimpleMessage(chatId, GET_BIRTHDATE);
            isWaitingDate = true;
        }
        else if(isWaitingDate){
            // обработка даты
            if(DateValidator.isValidDate(messageText)){
                LocalDate birthday = DateValidator.parseDate(messageText);
                // сохранение даты для пользователя в бд
                UserModel user = userRepo.findById(userId).orElseThrow();
                userRepo.save(user.toBuilder()
                        .birthday(birthday)
                        .build());
                isWaitingDate = false;
                sendGenderRequestMessage(chatId);
            }
            else {
                sendSimpleMessage(chatId, WRONG_FORMAT);
            }
        }
        else{
            sendSimpleMessage(chatId, WHAT);
        }
    }

    // для обработки сообщений без текста
    public void photoMessageHandler(Message message) throws TelegramApiException, IOException {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            //  get the largest photo (the best quality)
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            if (largestPhoto != null) {
                String photoId = largestPhoto.getFileId();
                sendSimpleMessage(chatId, THANK_FOR_ALL_DATA);

                File photoFile = telegramClient.execute(GetFile.builder().fileId(photoId).build());
                java.io.File downloadedFile = telegramClient.downloadFile(photoFile);

                SendDocument sendDocument = SendDocument.builder()
                        .chatId(chatId)
                        .document(fileService.getDocumentForUser(userId, downloadedFile))
                        .build();

                telegramClient.execute(sendDocument);
            } else {
                sendSimpleMessage(chatId, "Произошла ошибка при обработке фотографии.");
            }
        }
    }

    // обработка новых пользователей (полученние ссылки по которой перешли)
    public String startMessageHandler(String message){
        if(message.startsWith("/start") && !message.equals("/start")){
            String sourceUrl = botConfig.getBotUrl() + message.replace("/start ", "");
            log.info("\t User joined from source: {}", sourceUrl);
            return sourceUrl;
        }
        else return botConfig.getBotUrl();
    }

    // метод для отправки сообщения для получения согласия на обработку данных
    public void sendMessageToGetPolicyAgree(long chatId){
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chatId)
                .text("Для продолжения, нам необходимо ваше согласие на обработку персональных данных. Пожалуйста ознакомтесь и дайте согласие!")
                .build();
        InlineKeyboardRow row1 = new InlineKeyboardRow();

        InlineKeyboardButton inlineKeyboardButton = InlineKeyboardButton.builder()
                .text("Политика конфиденциальности")
                .url(botConfig.getPolicyUrl())
                .build();
        row1.add(inlineKeyboardButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton inlineKeyboardButton1 = InlineKeyboardButton.builder()
                .text("Даю согласие!")
                .callbackData("policy_agree")
                .build();
        row2.add(inlineKeyboardButton1);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of(row1, row2));
        messageToSend.setReplyMarkup(inlineKeyboardMarkup);

        execute(messageToSend);
    }

    public void sendGenderRequestMessage(long chatId){
        SendMessage messageToSend = SendMessage.builder()
                .chatId(chatId)
                .text("Пожалуйста укажите ваш пол:")
                .build();
        InlineKeyboardRow row = new InlineKeyboardRow();

        row.addAll(List.of(
                InlineKeyboardButton.builder()
                .text("Мужчина")
                .callbackData("male")
                .build(),
                InlineKeyboardButton.builder()
                .text("Женщина")
                .callbackData("female")
                .build()));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of(row));
        messageToSend.setReplyMarkup(inlineKeyboardMarkup);

        execute(messageToSend);
    }

    // вспомогательный метод для отправки кастомных сообщений
    public void execute(SendMessage sendMessage){
        try {
            // Send the message
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // приветствие
    public void startCommandHandler(long chatId, String name){
        String text = "Добрый день, " + name + "! Рады вас видеть в нашем боте!";
        sendSimpleMessage(chatId, text);
    }

    // для отправки обычных текстовых сообщений
    public void sendSimpleMessage(long chatId, String message){
        SendMessage send = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build();
        try{
            telegramClient.execute(send);
        }catch (TelegramApiException ignored){   }
    }


    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Registered bot running state is: {}", botSession.isRunning());
    }
}