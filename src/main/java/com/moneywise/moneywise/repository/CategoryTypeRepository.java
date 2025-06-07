package com.moneywise.moneywise.repository;



import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import com.moneywise.moneywise.entity.CategoryType;

@Repository
public interface CategoryTypeRepository extends MongoRepository<CategoryType, Integer> {


}
