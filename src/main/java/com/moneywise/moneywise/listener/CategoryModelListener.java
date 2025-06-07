package com.moneywise.moneywise.listener;

import com.moneywise.moneywise.entity.Category;
import com.moneywise.moneywise.service.SequenceGeneratorService;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
 
@Component
public class CategoryModelListener extends AbstractMongoEventListener<Category> {

    private final SequenceGeneratorService sequenceGenerator;


    public CategoryModelListener(SequenceGeneratorService sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void onBeforeConvert(@NonNull BeforeConvertEvent<Category> event) {
        Category category = event.getSource();
        if (category.getId() == null) {
            int id = (int) sequenceGenerator.generateSequence(Category.SEQUENCE_NAME);
            category.setId(id);
        }
    }
}