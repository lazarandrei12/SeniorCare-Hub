package org.example.healthapp.config;

import lombok.RequiredArgsConstructor;
import org.example.healthapp.models.Medic;
import org.example.healthapp.models.Supraveghetor;
import org.example.healthapp.repositories.MedicRepository;
import org.example.healthapp.repositories.SupraveghetorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final MedicRepository medicRepository;
    private final SupraveghetorRepository supraveghetorRepository;

    @Bean
    public CommandLineRunner initializareDate() {
        return args -> {
            if (medicRepository.count() == 0) {
                Medic medicImplicit = new Medic(
                        null,
                        "Popescu",
                        "Andrei",
                        "Cardiologie",
                        "0712345678",
                        "medic@spital.ro",
                        "parola123"
                );
                medicRepository.save(medicImplicit);
            }

            if (supraveghetorRepository.count() == 0) {
                Supraveghetor supraveghetorImplicit = new Supraveghetor(
                        null,
                        "Dispecerat",
                        "Spital",
                        "dispecerat@spital.ro",
                        "parola123"
                );
                supraveghetorRepository.save(supraveghetorImplicit);
            }
        };
    }
}

