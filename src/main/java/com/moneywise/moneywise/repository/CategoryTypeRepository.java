package com.moneywise.moneywise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import com.moneywise.moneywise.entity.CategoryType;

@Repository
public interface CategoryTypeRepository extends JpaRepository<CategoryType, Integer> {

    @Query("SELECT c FROM CategoryType c WHERE c.id = ?1")
    CategoryType findByCategoryTypeId(Integer id);
}
