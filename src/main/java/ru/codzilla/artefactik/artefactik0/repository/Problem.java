package ru.codzilla.artefactik.artefactik0.repository;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer timeLimit;

    @Column(nullable = false)
    private Integer memoryLimit;

    @Column(name = "statement_key")
    private String statementKey;

    @Column(name = "generator_key")
    private String generatorKey;

    @Column(name = "validator_key")
    private String validatorKey;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
