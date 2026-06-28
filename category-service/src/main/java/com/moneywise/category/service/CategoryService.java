package com.moneywise.category.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moneywise.category.repository.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryrepository;

    public Object getCategory() {
        try {
            return categoryrepository.findAll();
        } catch (Exception e) {
            return "Failed to fetch category: " + e.getLocalizedMessage();
        }
    }

}
