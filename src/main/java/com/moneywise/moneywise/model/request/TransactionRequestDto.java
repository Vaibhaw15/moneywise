package com.moneywise.moneywise.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto implements Comparable<TransactionRequestDto> {

    private Integer id;
    private Integer userId;
    private Integer txnAmount;
    private Integer txnCategoryId;
    private String txnDate;
    private String txnMessage;
    private Integer txnDateInt;
    private Integer isModify;
    private Integer modifyCount;


    public TransactionRequestDto(Integer userId, Integer txnAmount, Integer txnCategoryId,
                                  String txnDate, String txnMessage, Integer txnDateInt,
                                  Integer isModify, Integer modifyCount) {
        this.userId = userId;
        this.txnAmount = txnAmount;
        this.txnCategoryId = txnCategoryId;
        this.txnDate = txnDate;
        this.txnMessage = txnMessage;
        this.txnDateInt = txnDateInt;
        this.isModify = isModify;
        this.modifyCount = modifyCount;
    }


    @Override
    public int compareTo(TransactionRequestDto o) {
        return this.txnDateInt.compareTo(o.txnDateInt);
    }
    
}
