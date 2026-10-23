package com.ahadporkar.pruefpilot.auth;

import java.io.IOException;
import java.time.Clock;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.ahadporkar.pruefpilot.shared.Db;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    private static void error(HttpServletResponse response,int status,String code) throws IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":"+status+",\"code\":\""+code+"\"}");
    }
    @Bean SecurityFilterChain security(HttpSecurity http,Clock clock,Db db) throws Exception {
        LoginLimiter limiter=new LoginLimiter(clock);
        http.authorizeHttpRequests(auth->auth
            .requestMatchers("/api/auth/csrf","/api/auth/login","/actuator/health").permitAll()
            .requestMatchers("/api/**","/actuator/**").authenticated().anyRequest().permitAll());
        http.formLogin(form->form.loginProcessingUrl("/api/auth/login")
            .successHandler((request,response,auth)->response.setStatus(204))
            .failureHandler((request,response,ex)->error(response,401,"invalidCredentials")));
        http.logout(logout->logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("PP_SESSION")
            .logoutSuccessHandler((request,response,auth)->response.setStatus(204)));
        http.exceptionHandling(ex->ex.authenticationEntryPoint((req,res,e)->error(res,401,"unauthenticated"))
            .accessDeniedHandler((req,res,e)->error(res,403,"forbidden")));
        http.headers(headers->headers
            .contentSecurityPolicy(csp->csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' blob: data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'"))
            .permissionsPolicy(p->p.policy("camera=(self), microphone=(), geolocation=()")));
        http.addFilterBefore(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                if(request.getRequestURI().equals("/api/auth/login") && request.getMethod().equals("POST") && !limiter.allow(request.getRemoteAddr())) {
                    response.setHeader("Retry-After","60");error(response,429,"rateLimited");return;
                }
                chain.doFilter(request,response);
            }
        },UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                var authentication=SecurityContextHolder.getContext().getAuthentication();
                if(request.getRequestURI().startsWith("/api/") && authentication!=null && authentication.getPrincipal() instanceof Actor actor) {
                    long valid=db.count("SELECT count(*) FROM app_user WHERE id=? AND organization_id=? AND active AND role=? AND password_hash=?",actor.id(),actor.organizationId(),actor.role(),actor.passwordHash());
                    if(valid!=1) { var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();error(response,401,"unauthenticated");return; }
                }
                chain.doFilter(request,response);
            }
        },AuthorizationFilter.class);
        return http.build();
    }
}
