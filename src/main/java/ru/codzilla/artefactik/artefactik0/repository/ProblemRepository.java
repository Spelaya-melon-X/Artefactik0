package ru.codzilla.artefactik.artefactik0.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findAllByComplexity(Problem.TaskComplexity complexity) ;
    @Query(value = "SELECT * FROM problems WHERE complexity = :complexity ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Problem> findRandomByComplexity(@Param("complexity") String complexity);
}
