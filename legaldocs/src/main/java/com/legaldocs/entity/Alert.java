package com.legaldocs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obligation_id")
    private Obligation obligation;

    @Column(nullable = false)
    private String alertType; // DAYS_30, DAYS_15, DAYS_7, OBLIGATION_DUE

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private String status; // SENT, FAILED

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}
