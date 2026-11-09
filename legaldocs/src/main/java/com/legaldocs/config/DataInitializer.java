package com.legaldocs.config;

import com.legaldocs.entity.ContractType;
import com.legaldocs.entity.User;
import com.legaldocs.repository.ContractTypeRepository;
import com.legaldocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ContractTypeRepository contractTypeRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (contractTypeRepository.count() == 0) {
            contractTypeRepository.saveAll(List.of(
                ContractType.builder().name("Prestación de Servicios").description("Servicios profesionales y técnicos").build(),
                ContractType.builder().name("Confidencialidad (NDA)").description("Acuerdos de no divulgación").build(),
                ContractType.builder().name("Arrendamiento").description("Inmuebles, oficinas o equipos").build(),
                ContractType.builder().name("Suministro / Proveedores").description("Compra recurrente de insumos o licencias").build(),
                ContractType.builder().name("Laboral").description("Contratos de trabajo e incorporación").build()
            ));
        }

        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                .fullName("Administrador LegalDocs")
                .email("admin@legaldocs.com")
                .passwordHash("admin123")
                .role("ADMIN")
                .build());
        }
    }
}
