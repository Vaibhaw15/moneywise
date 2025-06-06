package com.moneywise.moneywise.service;
import java.util.HashMap;
import java.util.Map;
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


        Optional<User> existingUser = userRepo.findByEmail(email);
        if(existingUser.isPresent()){
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
        
        Optional<User> userData = userRepo.findByEmail(email);
        if(userData.isEmpty()) {
            throw new InvalidUserException("User Not Found!!");
        }

        if(!passwordEncoder.matches(password,userData.get().getPassword()))
        {
            throw new InvalidUserException("Invalid Password");
        } 
            
        
        // if(!userData.getPassword().equals(password)) throw new InvalidUserException("Invalid Password");
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", userData.get().getEmail());
        
        return Map.of("userId",userData.get().getId().toString(),
                      "userName",userData.get().getUsername().toString(),
                      "userEmail",userData.get().getEmail().toString(),
                      "token",jwtUtil.generateToken(userData.get().getEmail(),claims));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        User user = userRepo.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .roles("USER")
        .build();
    }


}
