package com.moneywise.moneywise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.moneywise.moneywise.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @Query("SELECT c FROM Category c WHERE c.categoryTypeId = ?1")
    Category findByCategoryTypeId(Integer categoryTypeId);
    
    @Query("SELECT c FROM Category c WHERE c.categoryTypeId IN (?1)")
    List<Category> findByCategoryTypeIdIn(List<Integer> categoryTypeId);
}
