package io.happysadalex.tgbot.repository;

import io.happysadalex.tgbot.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<UserModel, Long> {
}
