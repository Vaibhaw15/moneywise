package com.moneywise.moneywise.service;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moneywise.moneywise.entity.User;
import com.moneywise.moneywise.exceptions.InvalidUserException;
import com.moneywise.moneywise.model.request.UserDTO;
import com.moneywise.moneywise.repository.UserRepository;
import com.moneywise.moneywise.utils.JWTUtil;


@Service
public class UserService implements UserDetailsService{

    @Autowired
    private  UserRepository userRepo;
    

    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private  PasswordEncoder passwordEncoder;


    public String createUser(UserDTO user) throws Exception{

        String userName = Optional.ofNullable(user.getUserName()).orElseThrow(() -> new Exception("User Name is not present"));
        String email = Optional.ofNullable(user.getEmail()).orElseThrow(() -> new Exception("Email is not present"));
        String password = Optional.ofNullable(user.getPassword()).orElseThrow(() -> new Exception("Password is not present"));


        User existingUser = userRepo.findByEmail(email);
        if(Objects.nonNull(existingUser)){
            throw new InvalidUserException("User Email Already Exists");
        }

        User userData = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .username(userName)
                            .build();

        userData = userRepo.save(userData);
        

        return "User Register Successfull!!";
        
    }

 

    public Object loginUser(UserDTO user) throws Exception {
        String email = Optional.ofNullable(user.getEmail()).orElseThrow(() -> new Exception("Email is not present"));
        String password = Optional.ofNullable(user.getPassword()).orElseThrow(() -> new Exception("Password is not present"));
        
        User userData = userRepo.findByEmail(email);
        if(Objects.isNull(userData)) throw new InvalidUserException("User Not Found!!");

        if(!passwordEncoder.matches(password,userData.getPassword()))
        {
            throw new InvalidUserException("Invalid Password");
        } 
            
        
        // if(!userData.getPassword().equals(password)) throw new InvalidUserException("Invalid Password");
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", userData.getEmail());
        
        return Map.of("userId",userData.getId().toString(),
                      "userName",userData.getUsername().toString(),
                      "userEmail",userData.getEmail().toString(),
                      "token",jwtUtil.generateToken(userData.getUsername(),claims));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        User user = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .roles("USER")
        .build();
    }


}
