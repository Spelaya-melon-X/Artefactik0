package ru.codzilla.artefactik.artefactik0.repository;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "problem_tests")
public class ProblemTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long problemId;

    @Column(nullable = false)
    private Integer testIndex;

    @Column(nullable = false)
    private String inputKey;

    @Column(nullable = false)
    private String outputKey;

    private LocalDateTime createdAt = LocalDateTime.now();
}
