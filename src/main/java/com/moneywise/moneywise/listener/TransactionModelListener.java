package com.moneywise.moneywise.listener;

import com.moneywise.moneywise.entity.Transaction;
import com.moneywise.moneywise.service.SequenceGeneratorService;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
public class TransactionModelListener extends AbstractMongoEventListener<Transaction> {

    private final SequenceGeneratorService sequenceGenerator;

    public TransactionModelListener(SequenceGeneratorService sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void onBeforeConvert(@NonNull BeforeConvertEvent<Transaction> event) {
        Transaction transaction = event.getSource();
        // Only assign an ID if it's a new entity (ID is null)
        if (transaction.getId() == null) {
            int id = (int) sequenceGenerator.generateSequence(Transaction.SEQUENCE_NAME);
            transaction.setId(id);
        }
    }
}