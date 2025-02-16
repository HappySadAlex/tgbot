package io.happysadalex.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Document(collection = "users")
public class UserModel {

    @MongoId
    private Long id;

    private String firstName;

    private String secondName;

    private String surname;

    private LocalDate birthday;

    private Boolean policyAgreement;

    private String sex;

    private String photoId;

    private String sourceUrl;
}
