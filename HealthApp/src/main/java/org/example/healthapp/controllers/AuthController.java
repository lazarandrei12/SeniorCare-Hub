package org.example.healthapp.controllers;

import lombok.RequiredArgsConstructor;
import org.example.healthapp.dto.AuthResponse;
import org.example.healthapp.dto.LoginRequest;
import org.example.healthapp.models.Ingrijitor;
import org.example.healthapp.models.Medic;
import org.example.healthapp.models.Supraveghetor;
import org.example.healthapp.repositories.IngrijitorRepository;
import org.example.healthapp.repositories.MedicRepository;
import org.example.healthapp.repositories.SupraveghetorRepository;
import org.example.healthapp.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MedicRepository medicRepository;
    private final SupraveghetorRepository supraveghetorRepository;
    private final IngrijitorRepository ingrijitorRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest cerereLogin) {
        Optional<Medic> medicOptional = medicRepository.findByEmail(cerereLogin.getEmail());
        if (medicOptional.isPresent() && medicOptional.get().getParola().equals(cerereLogin.getParola())) {
            String token = jwtUtil.generateToken(medicOptional.get().getEmail());
            return ResponseEntity.ok(new AuthResponse(token));
        }

        Optional<Supraveghetor> supraveghetorOptional = supraveghetorRepository.findByEmail(cerereLogin.getEmail());
        if (supraveghetorOptional.isPresent() && supraveghetorOptional.get().getParola().equals(cerereLogin.getParola())) {
            String token = jwtUtil.generateToken(supraveghetorOptional.get().getEmail());
            return ResponseEntity.ok(new AuthResponse(token));
        }

        Optional<Ingrijitor> ingrijitorOptional = ingrijitorRepository.findByEmail(cerereLogin.getEmail());
        if (ingrijitorOptional.isPresent() && ingrijitorOptional.get().getParola().equals(cerereLogin.getParola())) {
            String token = jwtUtil.generateToken(ingrijitorOptional.get().getEmail());
            return ResponseEntity.ok(new AuthResponse(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credențiale incorecte");
    }
}

