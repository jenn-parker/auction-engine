package com.jennparker.auction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "security.cognito.enabled", havingValue = "true")
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                .antMatchers("/api/v1/internal/auctioneers/**").hasAuthority("SCOPE_auction:admin")
                .antMatchers("/api/v1/internal/**").hasAuthority("SCOPE_auction:settle")
                .antMatchers("/api/v1/lots/*/bids").hasAuthority("SCOPE_auction:bid")
                .antMatchers("/api/v1/auctions/**", "/api/v1/lots/**").hasAuthority("SCOPE_auction:manage")
                .antMatchers("/api/v1/customers/**").authenticated()
                .anyRequest().authenticated()
                .and()
                .oauth2ResourceServer().jwt();
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.cognito.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
