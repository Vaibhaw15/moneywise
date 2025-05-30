package com.moneywise.moneywise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneywise.moneywise.service.AppCategoryService;



@RestController
@RequestMapping("/app-category")
public class AppCategory {
    
    @Autowired
    private AppCategoryService appCategoryService;

    @GetMapping("/get")
    public ResponseEntity<Object> getAllCategory(){
        try{

            return new ResponseEntity<>(appCategoryService.getCategory(),HttpStatus.OK);

        }catch(Exception e){

             return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);

        }
    }

}
