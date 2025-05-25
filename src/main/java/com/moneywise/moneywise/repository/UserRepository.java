package com.moneywise.moneywise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.moneywise.moneywise.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User,Integer>{

    User findByEmail(String emailId);
    Optional<User> findByUsername(String userName);
    
}


