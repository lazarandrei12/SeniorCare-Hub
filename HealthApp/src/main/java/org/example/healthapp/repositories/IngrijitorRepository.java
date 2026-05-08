package org.example.healthapp.repositories;

import java.util.Optional;

import org.example.healthapp.models.Ingrijitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngrijitorRepository extends JpaRepository<Ingrijitor, Long> {

    Optional<Ingrijitor> findByEmail(String email);
}

