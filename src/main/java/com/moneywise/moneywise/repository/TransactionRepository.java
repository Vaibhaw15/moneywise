package com.moneywise.moneywise.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moneywise.moneywise.entity.Transaction;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("SELECT t FROM Transaction t WHERE t.userId = ?1 AND t.transactionDateInt BETWEEN ?2 AND ?3")
    List<Transaction> getTransactionHistory(Integer userId, Integer startDate, Integer endDate);

}

