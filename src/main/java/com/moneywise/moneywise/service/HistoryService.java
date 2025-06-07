package com.moneywise.moneywise.service;

import java.util.Comparator;
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
              
                List<HistoryResponseDTO> responseList = txn.stream().map(txnOne -> {

                    Category category = categoryIdNameMap.get(txnOne.getTransactionCategoryId());
                    String categoryName =  "Unknow Category";
                    String categoryTypeName =  "Unknown Type";
                    if(category!=null){
                        categoryName =  category.getCategoryName();
                        categoryTypeName = categoryTypeIdNameMap.get(category.getCategoryTypeId());
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
                            category!=null ? category.getCategoryTypeId():0,
                            categoryName, 
                            categoryTypeName 
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
