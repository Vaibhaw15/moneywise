package com.moneywise.moneywise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.moneywise.moneywise.model.request.TransactionRequestDto;
import com.moneywise.moneywise.service.TransactionService;


@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionService txnService;

    @PostMapping("/addEditTransaction")
    @ResponseBody
    public ResponseEntity<Object> addTxn(@RequestBody TransactionRequestDto txn){

        try{
            return new ResponseEntity<>(txnService.addTransaction(txn),HttpStatus.CREATED);
        }catch (Exception e){
            return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
       
    }
    
}
