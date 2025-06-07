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
    private Integer categoryType;
    private String categoryName;
    private String categoryTypeName;

    

}
