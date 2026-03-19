package ws.processos.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ProcessosSecurityConfig {

    @Bean
    SecurityFilterChain processosSecurityFilterChain(HttpSecurity http,
                                                     ProcessosJwtContextFilter jwtContextFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api-doc/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/processo/resumo/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
