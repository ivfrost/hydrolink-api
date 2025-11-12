package dev.ivfrost.hydro_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

// Non-nullable: firmware, technicalName

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "devices")
@Entity
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 17, min = 17)
    @Column(nullable = false, name = "mac_address", unique = true)
    private String macAddress;

    @Size(min = 1, max = 40)
    @Column(nullable = true)
    private String name; // User defined name

    @Size(max = 20)
    @Column(length = 20)
    private String location; // Latest known location

    @Size(max = 20)
    @Column(nullable = false)
    private String firmware; // Firmware version

    @Size(max = 40)
    @Column(name = "technical_name", nullable = false)
    private String technicalName; // Technical name

    @Column(length = 44, unique = true)
    private String hash; // Unique device hash for verification

    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")
    @Size(max = 15)
    @Column(nullable = true)
    private String ip;

    @Column(name = "created_at")
    private Instant createdAt; // Timestamp when the device was created

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    private Instant linkedAt;

    private Instant lastSeen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "display_order")
    private Integer displayOrder; // User-defined order for display purposes

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
    }
}