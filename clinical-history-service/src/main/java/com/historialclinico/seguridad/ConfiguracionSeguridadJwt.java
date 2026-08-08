package com.historialclinico.seguridad;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import java.util.Collection;

@Configuration
@ConditionalOnProperty(name = "app.seguridad.modo", havingValue = "jwt")
public class ConfiguracionSeguridadJwt {
    @Bean JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String emisor,
            @Value("${app.seguridad.audiencia}") String audiencia) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(emisor);
        OAuth2TokenValidator<Jwt> audienciaValida = new JwtClaimValidator<>("aud", claim ->
                claim instanceof Collection<?> valores ? valores.stream().map(String::valueOf).anyMatch(audiencia::equals)
                        : audiencia.equals(String.valueOf(claim)));
        if (decoder instanceof org.springframework.security.oauth2.jwt.NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(emisor), audienciaValida));
        }
        return decoder;
    }

    @Bean SecurityFilterChain seguridadJwt(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {})).build();
    }
}
