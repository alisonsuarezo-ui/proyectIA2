package com.legaldocs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContractType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
