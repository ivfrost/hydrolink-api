package dev.ivfrost.hydro_backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ivfrost.hydro_backend.converter.JsonNodeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Entity
public class User {

    public static enum Role {
        USER,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 5, max = 20)
    @Column(unique = true, nullable = false)
    private String username;

    // Password is write-only to prevent it from being serialized in responses
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 12, max = 60)
    @Column(nullable = false)
    private String password;

    @Size(min = 4, max = 40)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(min = 8, max = 50)
    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "profile_pic")
    @Size(max = 255)
    private String profilePictureUrl;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 100)
    @Column(columnDefinition = "text")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = true)
    private Instant deletedAt;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Size(min = 2, max = 2)
    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage = "es";

    @Convert(converter = JsonNodeConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private JsonNode settings = new ObjectMapper().createObjectNode();

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private List<Device> devices = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // Enforce preferredLanguage to be a 2-letter ISO code in lowercase
    public void setPreferredLanguage(String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.length() != 2) {
            throw new IllegalArgumentException("Preferred language must be a 2-letter ISO code.");
        }
        this.preferredLanguage = preferredLanguage.toLowerCase();
    }

    @PrePersist
    protected void onCreate() {
        if (profilePictureUrl == null) profilePictureUrl = "";
        if (phoneNumber == null) phoneNumber = "";
        if (address == null) address = "";
        if (lastLogin == null) lastLogin = Instant.now();
        if (settings == null) settings = new ObjectMapper().createObjectNode();
        if (notes == null) notes = "";
        if (devices == null) devices = new ArrayList<>();
    }
}
