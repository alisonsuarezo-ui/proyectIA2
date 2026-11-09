package com.legaldocs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clauses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clause {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category; // CONFIDENTIALITY, PENALTY, TERMINATION, PAYMENT, OBLIGATION, OTHER

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private Integer pageNumber;

    private String qdrantPointId;
}
