package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Errand;
import com.nottingham.mynottingham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrandRepository extends JpaRepository<Errand, Long> {
    List<Errand> findByRequester(User requester);
    List<Errand> findByProvider(User provider);
    List<Errand> findByStatus(Errand.ErrandStatus status);
    List<Errand> findByType(Errand.ErrandType type);

    @Query("SELECT e FROM Errand e LEFT JOIN FETCH e.requester LEFT JOIN FETCH e.provider ORDER BY e.createdAt DESC")
    List<Errand> findAll();
}
