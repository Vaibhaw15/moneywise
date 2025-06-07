package com.moneywise.moneywise.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public Object getUserHistory(Integer userId, Integer startDate, Integer endDate) {

        try {
            List<Transaction> txn = transactionRepository.findByUserIdAndTransactionDateIntBetween(userId, startDate,
                    endDate);

            if (txn == null || txn.isEmpty()) {
                return "No transaction history found for userId: " + userId;
            } else {

                Map<Integer, String> categoryIdNameMap = categoryRepository.findAll()
                        .stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

                Map<Integer, String> categoryTypeIdNameMap = categoryTypeRepository.findAll()
                        .stream().collect(Collectors.toMap(CategoryType::getId, CategoryType::getCategoryTypeName));
              
                return txn.stream().map(txnOne -> {

                    String categoryName = categoryIdNameMap.getOrDefault(txnOne.getTransactionCategoryId(),"Unknown Category");;
                    String categoryTypeName =  categoryTypeIdNameMap.getOrDefault(txnOne.getTransactionCategoryId(), "Unknown Type");

                    return new HistoryResponseDTO(
                            txnOne.getId(),
                            txnOne.getUserId(),
                            txnOne.getTransactionAmount(),
                            txnOne.getTransactionCategoryId(),
                            txnOne.getTransactionMessage(),
                            txnOne.getTransactionDate(),
                            txnOne.getTransactionDateInt(),
                            txnOne.getIsModify(),
                            txnOne.getTransactionModificationCount(),
                            txnOne.getTransactionCategoryId(),
                            categoryName, // Now potentially null
                            categoryTypeName // Now potentially null
                    );
                }).collect(Collectors.toList());
               responseList.sort(
                    Comparator.comparing(HistoryResponseDTO::getTransactionDateInt).reversed()
                        .thenComparing(HistoryResponseDTO::getId));
              return responseList;
            }

        } catch (Exception e) {
            return "Failed to fetch transaction history: " + e.getMessage();
        }
    }
}
