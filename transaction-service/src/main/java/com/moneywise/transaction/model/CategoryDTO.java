package com.moneywise.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Integer id;
    private String categoryName;
    private Integer categoryTypeId;
    private String categoryIcon;
    private boolean isActive;
}
