package com.moneywise.transaction.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moneywise.transaction.entity.Transaction;
import com.moneywise.transaction.model.response.HistoryResponseDTO;
import com.moneywise.transaction.client.CategoryClient;

import com.moneywise.transaction.repository.TransactionRepository;

import com.moneywise.transaction.model.CategoryDTO;
import com.moneywise.transaction.model.CategoryTypeDTO;

@Service
public class HistoryService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryClient categoryClient;

    public Object getUserHistory(Integer userId,Integer startDate,Integer endDate){
    
        try {
            List<Transaction> txn = transactionRepository.getTransactionHistory(userId, startDate, endDate);
            
            

            if (txn == null || txn.isEmpty()) {
                return "No transaction history found for userId: " + userId;
            }else{

                //List<Integer> categoryIds = txn.stream().map(Transaction::getTransactionCategoryId).toList();

                //List<Category> categories = categoryRepository.findByCategoryTypeIdIn(categoryIds);

                Map<Integer,CategoryDTO> categoryIdNameMap = categoryClient.getAllCategories()
                .stream().collect(Collectors.toMap(CategoryDTO::getId, Function.identity()));
                

                // List<Integer> categoryTypeIds = categories.stream().map(Category::getCategoryTypeId).toList();

                Map<Integer, String> categoryTypeIdNameMap = categoryClient.getAllCategoryTypes()
                        .stream().collect(Collectors.toMap(CategoryTypeDTO::getId, CategoryTypeDTO::getCategoryTypeName));

                List<HistoryResponseDTO> responseList =  txn.stream().map(txnOne -> {

                    CategoryDTO category = categoryIdNameMap.get(txnOne.getTransactionCategoryId());
                    String categoryName = category != null ? category.getCategoryName() : "Unknown";
                    String categoryTypeName = (category != null)
                            ? categoryTypeIdNameMap.getOrDefault(category.getCategoryTypeId(), "Unknown")
                            : "Unknown";

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
                            txnOne.getTransactionCategoryId().toString(),
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
