package com.moneywise.moneywise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneywise.moneywise.service.LandingService;

@RestController
@RequestMapping("/dashboard")
public class LandingController {
    
    @Autowired
    private LandingService landingService; 
    
    @GetMapping
    public ResponseEntity<Object> getDashboard(@RequestParam Integer userId,
                                                @RequestParam String startDate,
                                                @RequestParam String endDate){
        try{
           return new ResponseEntity<>(landingService.getUserMonthlyStats(userId,startDate,endDate),HttpStatus.OK);
        }catch (Exception e){
           return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
    }
}