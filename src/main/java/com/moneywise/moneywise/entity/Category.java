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
@Document(collection  = "app_category")
public class Category {

    public static final String SEQUENCE_NAME = "category_sequence";

    @Id
    private Integer id;

    private String categoryName;

    private Integer categoryTypeId;

    private String categoryIcon;

    private boolean isActive;
}
