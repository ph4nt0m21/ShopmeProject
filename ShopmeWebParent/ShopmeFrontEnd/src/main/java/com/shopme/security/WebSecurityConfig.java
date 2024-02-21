package com.shopme.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import com.shopme.security.oauth.CustomerOAuth2UserService;
import com.shopme.security.oauth.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired private CustomerOAuth2UserService oAuth2UserService;
    @Autowired private OAuth2LoginSuccessHandler oAuth2LoginHandler;
    @Autowired private DatabaseLoginSuccessHandler databaseLoginHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize)->authorize
                .requestMatchers("/account_details", "/update_account_details", "/orders/**",
                        "/cart", "/address_book/**", "/checkout", "/place_order", "/reviews/**",
                        "/process_paypal_order", "/write_review/**", "/post_review").authenticated()
                .requestMatchers("/create_order").permitAll()
                .anyRequest().permitAll());

        http.formLogin(formLogin->formLogin
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(databaseLoginHandler)
                .permitAll());

        http.oauth2Login(oauth2Login->oauth2Login
                .loginPage("/login")
                .userInfoEndpoint(userInfoEndpoint->userInfoEndpoint
                .userService(oAuth2UserService))
                .successHandler(oAuth2LoginHandler));

        http.logout(logout->logout
                .permitAll());

        http.rememberMe(rememberMe -> rememberMe
                .key("AbcDefgHijKlmnOpqrs_1234567890")
                .tokenValiditySeconds(7 * 24 * 60 * 60));

        http.sessionManagement(sessionManagement->sessionManagement
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }

    public void configure(WebSecurity web){
        web.ignoring().requestMatchers("/images/**", "/js/**", "/webjars/**");
    }


    @Bean
    public UserDetailsService userDetailsService(){
        return new CustomerUserDetailsService();
    }

    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }
}
