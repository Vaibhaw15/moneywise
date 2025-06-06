package com.moneywise.moneywise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneywise.moneywise.entity.Transaction;
import com.moneywise.moneywise.model.request.TransactionRequestDto;
import com.moneywise.moneywise.repository.TransactionRepository;

@Service
@Transactional(rollbackFor = Exception.class)
public class TransactionService {

    private static final String SUCCESS = "SUCCESS";
    
    @Autowired
    private TransactionRepository transactionRepository;

    public String addTransaction(TransactionRequestDto txn){
        try{
            Transaction transaction = Transaction.builder()
                    .transactionAmount(txn.getTxnAmount())
                    .transactionCategoryId(txn.getTxnCategoryId())
                    .transactionDate(txn.getTxnDate())
                    .transactionDateInt(txn.getTxnDateInt())
                    .transactionMessage(txn.getTxnMessage())
                    .userId(txn.getUserId())
                    .id(txn.getId())
                    .isModify(txn.getIsModify())
                    .transactionModificationCount(txn.getModifyCount())
                    .build();

            transactionRepository.save(transaction);
        } catch(Exception e){
            return "Error Occurred while saving transaction";
        }

        return SUCCESS;
    }
}
