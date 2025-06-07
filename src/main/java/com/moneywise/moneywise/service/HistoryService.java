package com.moneywise.moneywise.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

                Map<Integer, Category> categoryIdNameMap = categoryRepository.findAll()
                        .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

                Map<Integer, String> categoryTypeIdNameMap = categoryTypeRepository.findAll()
                        .stream().collect(Collectors.toMap(CategoryType::getId, CategoryType::getCategoryTypeName));

                return txn.stream().map(txnOne -> {

                    Category category = categoryIdNameMap.get(txnOne.getTransactionCategoryId());
                    String categoryName = null;
                    String categoryTypeName = null;

                    // Perform lookups only if the category was found
                    if (category != null) {
                        // Get the category name
                        categoryName = category.getCategoryName();
                        if (category.getCategoryTypeId() != null) {
                            categoryTypeName = categoryTypeIdNameMap.get(category.getCategoryTypeId());
                        }
                    }

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
            }

        } catch (Exception e) {
            return "Failed to fetch transaction history: " + e.getMessage();
        }
    }
}
