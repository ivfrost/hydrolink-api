package dev.ivfrost.hydro_backend.users.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class UserRoleId implements Serializable {
  private Long userId;
  private UserRole.Role role;
}

@Entity
@Table(name = "user_roles")
@AllArgsConstructor
@NoArgsConstructor
@Data
@IdClass(UserRoleId.class)
public class UserRole {
  public enum Role {
    USER, ADMIN
  }

  public UserRole(User user, Role role) {
    this.user = user;
    this.role = role;
    this.userId = user.getId();
  }

  @Id
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;
}
