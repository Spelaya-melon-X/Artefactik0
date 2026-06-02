package ru.codzilla.artefactik.artefactik0.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemTestRepository extends JpaRepository<ProblemTest, Long> {
    List<ProblemTest> findAllByProblemIdOrderByTestIndex(Long problemId);
    void deleteAllByProblemId(Long problemId);
    Optional<ProblemTest> findByProblemIdAndTestIndex(Long problemId, int testIndex);
}
