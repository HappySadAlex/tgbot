package io.happysadalex.tgbot.service;

import io.happysadalex.tgbot.model.UserModel;
import io.happysadalex.tgbot.repository.UserRepository;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.*;


@Service
public class FileService {

    private final UserRepository userRepo;

    public FileService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public InputFile getDocumentForUser(Long userId, File photoFile) throws IOException {
        File file = new File("form");
        UserModel userModel = userRepo.findById(userId).orElseThrow();
        String userFio = userModel.getSecondName() + " " + userModel.getFirstName() + " " + userModel.getSurname();

        XWPFDocument document = new XWPFDocument();
        // Создаем параграфы и заполняем данными
        XWPFParagraph fullNameParagraph = document.createParagraph();
        XWPFRun fullNameRun = fullNameParagraph.createRun();
        fullNameRun.setText("ФИО: " + userFio);
        fullNameRun.addBreak();

        XWPFParagraph birthdateParagraph = document.createParagraph();
        XWPFRun birthdateRun = birthdateParagraph.createRun();
        birthdateRun.setText("Дата рождения: " + userModel.getBirthday());
        birthdateRun.addBreak();

        XWPFParagraph genderParagraph = document.createParagraph();
        XWPFRun genderRun = genderParagraph.createRun();
        genderRun.setText("Пол: " + userModel.getSex());
        genderRun.addBreak();

        // Добавляем фотографию (если есть)
        if (photoFile != null && photoFile.exists()) {
            try (FileInputStream imageStream = new FileInputStream(photoFile)) {
                XWPFParagraph photoParagraph = document.createParagraph();
                XWPFRun photoRun = photoParagraph.createRun();
                photoRun.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_JPEG, photoFile.getName(), Units.toEMU(200), Units.toEMU(200));
                photoRun.addBreak();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        document.close(); // Close the document

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        // Создаем InputFile для отправки в Telegram
        InputFile inputFile = new InputFile(inputStream, "user_data.docx");

        return inputFile;
    }

}
