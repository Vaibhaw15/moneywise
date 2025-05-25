package com.moneywise.moneywise.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneywise.moneywise.entity.Transaction;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Integer>{
    
}
