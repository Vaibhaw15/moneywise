package com.moneywise.moneywise.listener;

import com.moneywise.moneywise.entity.CategoryType; // Import your entity
import com.moneywise.moneywise.service.SequenceGeneratorService;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
public class CategoryTypeModelListener extends AbstractMongoEventListener<CategoryType> {

    private final SequenceGeneratorService sequenceGenerator;

    public CategoryTypeModelListener(SequenceGeneratorService sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void onBeforeConvert(@NonNull BeforeConvertEvent<CategoryType> event) {
        CategoryType categoryType = event.getSource();
        if (categoryType.getId() == null) {
            int id = (int) sequenceGenerator.generateSequence(CategoryType.SEQUENCE_NAME);
            categoryType.setId(id);
        }
    }
}