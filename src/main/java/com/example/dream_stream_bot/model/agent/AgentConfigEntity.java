package com.example.dream_stream_bot.model.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_config")
@Data
public class AgentConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentRole role = AgentRole.CONVERSATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentProvider provider = AgentProvider.OPENAI;

    @Column(nullable = false, length = 64)
    private String model = "gpt-4o";

    private Double temperature;

    @Column(name = "top_p")
    private Double topP;
    @Column(name = "frequency_penalty")
    private Double frequencyPenalty;
    @Column(name = "presence_penalty")
    private Double presencePenalty;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "mem_window")
    private Integer memWindow = 100;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
