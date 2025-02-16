package io.happysadalex.tgbot.util;

import lombok.experimental.UtilityClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@UtilityClass
public class DateValidator {

    public static boolean isValidDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        dateFormat.setLenient(false); // Строгая проверка формата

        try {
            Date date = dateFormat.parse(dateString);
            return true; // Дата соответствует формату и является корректной
        } catch (ParseException e) {
            return false; // Дата не соответствует формату или является некорректной
        }
    }

    public static LocalDate parseDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return LocalDate.parse(dateString, formatter);
    }
}