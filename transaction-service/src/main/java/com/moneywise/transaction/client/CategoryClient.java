package com.moneywise.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import com.moneywise.transaction.model.CategoryDTO;
import com.moneywise.transaction.model.CategoryTypeDTO;

import java.util.List;

@FeignClient(name = "category-service", path = "/api/v1/category", fallback = CategoryClientFallback.class)
public interface CategoryClient {

    @GetMapping("/get")
    List<CategoryDTO> getAllCategories();

    @GetMapping("/types")
    List<CategoryTypeDTO> getAllCategoryTypes();
}
