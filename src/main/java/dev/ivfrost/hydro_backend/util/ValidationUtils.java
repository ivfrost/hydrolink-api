package dev.ivfrost.hydro_backend.util;

import dev.ivfrost.hydro_backend.repository.UserRepository;
import dev.ivfrost.hydro_backend.service.UserService;
import jakarta.validation.Validation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class ValidationUtils {

    private final UserRepository userRepository;

    public Map<String, Object> getClassValidationRules(Class<?> className) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var beanDescriptor = validator.getConstraintsForClass(className);
        return beanDescriptor.getConstrainedProperties()
                .stream()
                .collect(Collectors.toMap(
                        prop -> prop.getPropertyName(),
                        prop -> prop.getConstraintDescriptors()
                                .stream()
                                .map(descriptor -> Map.of(
                                        "annotation", descriptor.getAnnotation().annotationType().getSimpleName(),
                                        "attributes", descriptor.getAttributes()
                                ))
                                .collect(Collectors.toList())
                ));
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
