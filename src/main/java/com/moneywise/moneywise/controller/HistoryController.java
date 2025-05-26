package com.moneywise.moneywise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneywise.moneywise.service.HistoryService;

@RestController
@RequestMapping("/transaction-history")
public class HistoryController {
    
    @Autowired
    private HistoryService historyService; 
    
    @GetMapping("/get")
    public ResponseEntity<Object> getTransactionHistory(@RequestParam Integer userId,
                                                @RequestParam Integer date){
        try{
           return new ResponseEntity<>(historyService.getUserHistory(userId,date),HttpStatus.OK);
        }catch (Exception e){
           return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
    }
}
