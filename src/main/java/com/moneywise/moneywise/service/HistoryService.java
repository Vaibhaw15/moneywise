package com.moneywise.moneywise.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moneywise.moneywise.entity.Transaction;
import com.moneywise.moneywise.model.response.HistoryResponseDTO;
import com.moneywise.moneywise.repository.CategoryRepository;
import com.moneywise.moneywise.repository.CategoryTypeRepository;
import com.moneywise.moneywise.repository.TransactionRepository;
import com.moneywise.moneywise.entity.Category;
import com.moneywise.moneywise.entity.CategoryType;


@Service
public class HistoryService {
    

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryTypeRepository categoryTypeRepository;

    public Object getUserHistory(Integer userId,Integer date){
    
        try {
            List<Transaction> txn = transactionRepository.getTransactionHistory(userId, date);

            if (txn == null || txn.isEmpty()) {
                return "No transaction history found for userId: " + userId;
            }else{
                return txn.stream().map(txnOne -> {

                Integer categoryTypeId = txnOne.getTransactionCategoryId();
                 
                Category category = categoryRepository.findByCategoryTypeId(categoryTypeId);

                CategoryType categoryType = categoryTypeRepository.findByCategoryTypeId(category.getCategoryTypeId());

                return new HistoryResponseDTO(
                    txnOne.getId(),
                    txnOne.getUserId(),
                    txnOne.getTransactionAmount(),
                    txnOne.getTransactionCategoryId(),
                    txnOne.getTransactionMessage(),
                    txnOne.getTransactionDate().toString(),
                    txnOne.getTransactionDateInt(),
                    txnOne.getIsModify(),
                    txnOne.getTransactionModificationCount(),
                    categoryTypeId.toString(),
                    category.getCategoryName(),
                    categoryType.getCategoryTypeName()
                          );
                    }).collect(Collectors.toList());
            }

        } catch (Exception e) {
            return "Failed to fetch transaction history: " + e.getMessage();
        }
    }
}
