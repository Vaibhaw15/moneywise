package com.moneywise.moneywise.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;

import com.moneywise.moneywise.entity.Transaction;


@Repository
public interface TransactionRepository extends MongoRepository<Transaction, Integer> {

    @Query("{ 'userId': ?0, 'transactionDateInt': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findByUserIdAndTransactionDateIntBetween(Integer userId, Integer startDate, Integer endDate);
}
