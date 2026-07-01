package dev.ivfrost.hydro_backend.users.internal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "users")
@Entity
public class User implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Size(min = 5, max = 20)
  @Column(unique = true, nullable = false)
  private String username;

  @Size(max = 255)
  @Column(nullable = false)
  private String password;

  @Size(min = 5, max = 40)
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

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ElementCollection
  @Enumerated(EnumType.STRING)
  @Fetch(FetchMode.JOIN)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role", nullable = false)
  private List<Role> roles;

  @Column(name = "settings", columnDefinition = "text")
  private String settings;

  @Column(columnDefinition = "text")
  private String notes;

  @Builder.Default
  @Column(name = "is_enabled", nullable = false)
  private boolean isEnabled = true;

  public enum Role {
    USER, ADMIN
  }

}

@ImportRuntimeHints(value = UserRuntimeHints.class)
class UserRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(
        User.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}