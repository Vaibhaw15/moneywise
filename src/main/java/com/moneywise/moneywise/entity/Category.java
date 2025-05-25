package com.moneywise.moneywise.entity;

import jakarta.annotation.Generated;
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
@Table(name = "app_category")
public class Category {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Integer id;


    @Column(name = "category_name",nullable = false)
    private String categoryName;

    @Column(name = "category_type_id",nullable = false)
    private Integer categoryTypeId;

    @Column(name = "category_icon",nullable = false)
    private String categoryIcon;

    @Column(name = "isActive",nullable = false)
    private boolean isActive;
}
