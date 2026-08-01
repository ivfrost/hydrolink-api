package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRole;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

  UserResponse userToUserResponse(User user);

  @Mapping(target = "password", ignore = true)
  User userRegisterRequestToUser(UserRegisterRequest userRegisterRequest);

  // Ignore manually validated properties
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "password", ignore = true)
  void updateUserFromRequest(UserUpdateRequest req, @MappingTarget User user);

  default List<String> mapRoles(List<UserRole> roles) {
    return roles.stream().map(r -> r.getRole().toString()).toList();
  }
}