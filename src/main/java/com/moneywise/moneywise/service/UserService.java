package com.moneywise.moneywise.service;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moneywise.moneywise.entity.User;
import com.moneywise.moneywise.exceptions.InvalidUserException;
import com.moneywise.moneywise.model.request.UserDTO;
import com.moneywise.moneywise.repository.UserRepository;


@Service
public class UserService {

    private final UserRepository userRepo;
    
    UserService(UserRepository userRepo){
        this.userRepo = userRepo;
    };

    public Map<String,String> createUser(UserDTO user) throws Exception{

        String userName = Optional.ofNullable(user.getUserName()).orElseThrow(() -> new Exception("User Name is not present"));
        String email = Optional.ofNullable(user.getEmail()).orElseThrow(() -> new Exception("Email is not present"));
        String password = Optional.ofNullable(user.getPassword()).orElseThrow(() -> new Exception("Password is not present"));

        User userData = User.builder()
                            .email(email)
                            .password(password)
                            .username(userName)
                            .build();

        userData = userRepo.save(userData);
        

        return Map.of("userId",userData.getId().toString(),
                      "userName",userData.getUsername().toString(),
                      "userEmail",userData.getEmail().toString());
        
    }

 

    public Object loginUser(UserDTO user) throws Exception {
        String email = Optional.ofNullable(user.getEmail()).orElseThrow(() -> new Exception("Email is not present"));
        String password = Optional.ofNullable(user.getPassword()).orElseThrow(() -> new Exception("Password is not present"));
        
        User userData = userRepo.findByEmail(email);
        if(Objects.isNull(userData)) throw new InvalidUserException("User Not Found!!");
        
        if(!userData.getPassword().equals(password)) throw new InvalidUserException("Invalid Password");
        
        
        return Map.of("userId",userData.getId().toString(),
                      "userName",userData.getUsername().toString(),
                      "userEmail",userData.getEmail().toString());
    }


}
