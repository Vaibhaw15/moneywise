package com.moneywise.moneywise.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.moneywise.moneywise.entity.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    Category findByCategoryTypeId(Integer categoryTypeId);

    List<Category> findByCategoryTypeIdIn(List<Integer> categoryTypeId);
}
