package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "bot_knowledge_base")
@Getter
@Setter
public class BotKnowledgeBaseEntity {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    private String intentCode;
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Boolean isActive = true;
}