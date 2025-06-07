package com.moneywise.moneywise.listener;

import com.moneywise.moneywise.entity.User; // Import your entity
import com.moneywise.moneywise.service.SequenceGeneratorService;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
public class UserModelListener extends AbstractMongoEventListener<User> {

    private final SequenceGeneratorService sequenceGenerator;

    public UserModelListener(SequenceGeneratorService sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void onBeforeConvert(@NonNull BeforeConvertEvent<User> event) {
        User user = event.getSource();
        if (user.getId() == null) {
            int id = (int) sequenceGenerator.generateSequence(User.SEQUENCE_NAME);
            user.setId(id);
        }
    }
}