package org.example.healthapp.repositories;

import java.util.Optional;

import org.example.healthapp.models.Supraveghetor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupraveghetorRepository extends JpaRepository<Supraveghetor, Long> {

    Optional<Supraveghetor> findByEmail(String email);
}

