/**
 * Cross-cutting infrastructure: security wiring, AWS/SQS and object-store configuration, the command
 * gateway seam, shared property records, and the global exception handler.
 *
 * <p>This module is declared {@link org.springframework.modulith.ApplicationModule.Type#OPEN}
 * because it is deliberately allowed to depend on every domain module (security needs the user
 * converter, the exception handler knows every module's exceptions). Without OPEN, those references
 * form a cycle with the domain modules that depend on {@code config} for their own wiring.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package dev.ivfrost.hydro_backend.config;
