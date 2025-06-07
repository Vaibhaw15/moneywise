package com.moneywise.moneywise.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
@Document(collection = "app_transaction")
public class Transaction {

    public static final String SEQUENCE_NAME = "transaction_sequence";

    @Id
    private Integer id;

    private Integer userId;


    private Integer transactionAmount;

    private Integer transactionCategoryId;

    private String transactionMessage;
    
    private String transactionDate;

    private Integer transactionDateInt;

    private Integer isModify;

    private Integer transactionModificationCount;
}
