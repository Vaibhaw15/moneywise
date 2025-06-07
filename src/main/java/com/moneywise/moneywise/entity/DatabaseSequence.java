package com.moneywise.moneywise.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "database_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSequence {
    @Id
    private String id; // This ID will be the name of the collection's sequence (e.g., "category_sequence", "user_sequence")
    private long seq;  // The actual sequence number
}