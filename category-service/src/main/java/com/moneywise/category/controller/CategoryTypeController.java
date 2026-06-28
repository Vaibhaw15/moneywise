package com.moneywise.category.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.moneywise.category.repository.CategoryTypeRepository;

@RestController
public class CategoryTypeController {

    @Autowired
    private CategoryTypeRepository categoryTypeRepository;

    @GetMapping("/types")
    public ResponseEntity<Object> getAllCategoryTypes() {
        try {
            return new ResponseEntity<>(categoryTypeRepository.findAll(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
