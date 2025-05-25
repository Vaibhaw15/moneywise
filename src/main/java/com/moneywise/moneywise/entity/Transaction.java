package com.moneywise.moneywise.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Builder
@Table(name = "app_transaction")
public class Transaction {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Integer id;

    @Column(name = "user_id",nullable = false)
    private Integer userId;


    @Column(name = "transaction_amount",nullable = false)
    private Integer transactionAmount;

    @Column(name = "transaction_category_id",nullable = false)
    private Integer transactionCategoryId;


    @Column(name = "transaction_message",nullable = false)
    private String transactionMessage;
    
    @Column(name = "transaction_date",nullable = false)
    private String transactionDate;

    @Column(name = "transaction_date_int",nullable = false)
    private Integer transactionDateInt;

    @Column(name = "is_modifiy",nullable = false)
    private Integer isModify;

    @Column(name = "transaction_modification_count",nullable = false)
    private Integer transactionModificationCount;
}
