package bashkirov.store_original.config;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SneakyThrows
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity
                .authorizeRequests(authorizeRequest ->
                        authorizeRequest
                                .requestMatchers("/admin/**", "/telegram/**", "/order/admin/**").hasRole("ADMIN")
                                .requestMatchers("/manager/**").hasRole("MANAGER")
                                .requestMatchers(HttpMethod.GET, "/product/**").permitAll()
                                .requestMatchers("/auth/**", "/activate/**", "/error").permitAll()
                                .anyRequest().authenticated()
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/auth/login")
                                .loginProcessingUrl("/process_login")
                                .defaultSuccessUrl("/product", true)
                                .failureUrl("/auth/login?error")
                )
//                .exceptionHandling(exceptionHandling ->
//                        exceptionHandling
//                                .accessDeniedHandler((request, response, accessDeniedException) -> {
//                                    response.sendRedirect("/auth/login");
//                                })
//                );
        ;
        return httpSecurity.build();
    }
}
