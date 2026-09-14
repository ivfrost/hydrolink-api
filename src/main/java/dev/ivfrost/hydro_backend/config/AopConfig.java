package dev.ivfrost.hydro_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Forces class-based (CGLIB) proxies for all Spring AOP infrastructure to avoid JDK dynamic proxies
 * wrapping filters, which can break GenericFilterBean initialization on startup.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {

}
