package com.moneywise.moneywise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.moneywise.moneywise.exceptions.InvalidUserException;
import com.moneywise.moneywise.model.request.UserDTO;
import com.moneywise.moneywise.service.UserService;




@Controller
@RequestMapping("/auth")
public class LoginController {
   
    @Autowired
    private UserService userService;
    
    @RequestMapping(value="/login", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> loginUser(@RequestBody UserDTO user) {
        try{
           return ResponseEntity.ok(userService.loginUser(user));
        } catch (InvalidUserException e){
            return new ResponseEntity<>("Invalid User",HttpStatus.BAD_REQUEST);
        } catch(Exception e){
            return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/createUser")
    @ResponseBody
    public ResponseEntity<Object> createUser(@RequestBody UserDTO user) {
        try {
            return new ResponseEntity<>(userService.createUser(user),HttpStatus.CREATED);
        } catch (InvalidUserException e){
              return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.CONFLICT);
        }catch (Exception e){
            return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
    }

    
}
