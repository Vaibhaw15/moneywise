package com.moneywise.moneywise.model.response;

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
public class HistoryResponseDTO {
    private Integer id;
    private Integer userId;
    private Integer transactionAmount;
    private Integer transactionCategoryId;
    private String transactionMessage;
    private String transactionDate;
    private Integer transactionDateInt;
    private Integer isModify;
    private Integer transactionModificationCount;
    private String categoryType;
    private String categoryName;
    private String categoryTypeName;

    public HistoryResponseDTO(
        Integer id, int userId, Integer transactionAmount, Integer transactionCategoryId,
        String transactionMessage, String transactionDate, Integer transactionDateInt,
        Integer isModify, Integer transactionModificationCount, String categoryType,
         String categoryName, String categoryTypeName) {

        this.id = id;
        this.userId = userId;
        this.transactionAmount = transactionAmount;
        this.transactionCategoryId = transactionCategoryId;
        this.transactionMessage = transactionMessage;
        this.transactionDate = transactionDate;
        this.transactionDateInt = transactionDateInt;
        this.isModify = isModify;
        this.transactionModificationCount = transactionModificationCount;
        this.categoryType = categoryType;
        this.categoryName = categoryName;
        this.categoryTypeName = categoryTypeName;
    }

}
