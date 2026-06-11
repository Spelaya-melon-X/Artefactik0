package ru.codzilla.artefactik.artefactik0.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findAllByComplexity(Problem.TaskComplexity complexity) ;
}
