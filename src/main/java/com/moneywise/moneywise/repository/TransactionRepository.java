package com.moneywise.moneywise.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.moneywise.moneywise.entity.Transaction;


@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByUserIdAndTransactionDateIntBetween(Integer userId, Integer startDate, Integer endDate);
}


