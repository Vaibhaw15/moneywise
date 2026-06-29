package com.moneywise.transaction.client;

import com.moneywise.transaction.model.CategoryDTO;
import com.moneywise.transaction.model.CategoryTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for CategoryClient.
 * When the category-service is down or the circuit breaker is OPEN,
 * these methods return safe default values instead of throwing exceptions.
 */
@Slf4j
@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public List<CategoryDTO> getAllCategories() {
        log.warn("Circuit Breaker OPEN — category-service is unavailable. Returning empty categories list.");
        return Collections.emptyList();
    }

    @Override
    public List<CategoryTypeDTO> getAllCategoryTypes() {
        log.warn("Circuit Breaker OPEN — category-service is unavailable. Returning empty category types list.");
        return Collections.emptyList();
    }
}
