package ru.codzilla.artefactik.artefactik0.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findAllByComplexity(Problem.TaskComplexity complexity);

    List<Problem> findAllByTaskType(Problem.TaskType taskType);

    List<Problem> findAllByComplexityAndTaskType(
            Problem.TaskComplexity complexity, Problem.TaskType taskType);

    @Query(value = "SELECT * FROM problems WHERE complexity = :complexity ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true)
    Optional<Problem> findRandomByComplexity(@Param("complexity") String complexity);

    @Query(value = "SELECT * FROM problems WHERE task_type = :taskType AND complexity = :complexity ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true)
    Optional<Problem> findRandomByTaskTypeAndComplexity(
            @Param("taskType") String taskType,
            @Param("complexity") String complexity);

    @Query(value = "SELECT * FROM problems WHERE task_type = :taskType ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true)
    Optional<Problem> findRandomByTaskType(@Param("taskType") String taskType);
}