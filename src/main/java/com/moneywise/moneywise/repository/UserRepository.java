package com.moneywise.moneywise.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.moneywise.moneywise.entity.User;


@Repository
public interface UserRepository extends MongoRepository<User,String>{

    User findByEmail(String emailId);
    Optional<User> findByUsername(String userName);
    
}


