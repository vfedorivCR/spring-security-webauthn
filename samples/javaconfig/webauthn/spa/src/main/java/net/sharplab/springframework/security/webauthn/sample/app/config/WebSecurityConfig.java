package net.sharplab.springframework.security.webauthn.sample.app.config;

import com.webauthn4j.registry.Registry;
import com.webauthn4j.request.PublicKeyCredentialType;
import com.webauthn4j.response.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidator;
import net.sharplab.springframework.security.webauthn.WebAuthnRegistrationRequestValidator;
import net.sharplab.springframework.security.webauthn.authenticator.FidoServerAuthenticatorService;
import net.sharplab.springframework.security.webauthn.config.configurers.WebAuthnAuthenticationProviderConfigurer;
import net.sharplab.springframework.security.webauthn.userdetails.WebAuthnUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.mfa.MultiFactorAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static net.sharplab.springframework.security.webauthn.config.configurers.FidoServerConfigurer.fidoServer;
import static net.sharplab.springframework.security.webauthn.config.configurers.WebAuthnConfigurer.webAuthn;
import static net.sharplab.springframework.security.webauthn.config.configurers.WebAuthnLoginConfigurer.webAuthnLogin;


/**
 * Security Configuration
 */
@Configuration
@Import(value = WebSecurityBeanConfig.class)
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private AuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    @Autowired
    private LogoutSuccessHandler logoutSuccessHandler;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private DaoAuthenticationProvider daoAuthenticationProvider;

    @Autowired
    private WebAuthnUserDetailsService userDetailsService;

    @Autowired
    private FidoServerAuthenticatorService authenticatorService;

    @Autowired
    private WebAuthnAuthenticationContextValidator webAuthnAuthenticationContextValidator;

    @Autowired
    private WebAuthnRegistrationRequestValidator webAuthnRegistrationRequestValidator;

    @Autowired
    private Registry registry;

    @Override
    public void configure(AuthenticationManagerBuilder builder) throws Exception {
        builder.apply(new WebAuthnAuthenticationProviderConfigurer<>(userDetailsService, authenticatorService, webAuthnAuthenticationContextValidator));
        builder.apply(new MultiFactorAuthenticationProviderConfigurer<>(daoAuthenticationProvider));
    }

    @Override
    public void configure(WebSecurity web) {
        // ignore static resources
        web.ignoring().antMatchers(
                "/favicon.ico",
                "/static/**",
                "/webjars/**",
                "/angular",
                "/angular/**");
    }

    /**
     * Configure SecurityFilterChain
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // WebAuthn Config
        http.apply(webAuthn())
                .rpName("Spring Security WebAuthn Sample")
                .publicKeyCredParams()
                    .addPublicKeyCredParams(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)  // Windows Hello
                    .addPublicKeyCredParams(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256); // FIDO U2F Key, etc

        // FIDO Server Endpoints
        http.apply(fidoServer())
                .fidoServerAttestationOptionsEndpoint()
                .and()
                .fidoServerAttestationResultEndpointConfig()
                    .fidoServerAuthenticatorService(authenticatorService)
                    .webAuthnRegistrationRequestValidator(webAuthnRegistrationRequestValidator)
                .and()
                .fidoServerAssertionOptionsEndpointConfig()
                .and()
                .fidoServerAssertionResultEndpoint();

        // WebAuthn Login
        http.apply(webAuthnLogin())
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .credentialIdParameter("credentialId")
                .clientDataParameter("clientData")
                .authenticatorDataParameter("authenticatorData")
                .signatureParameter("signature")
                .clientExtensionsJSONParameter("clientExtensionsJSON")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler);

        // Logout
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler);

        // Authorization
        http.authorizeRequests()
                .mvcMatchers("/").permitAll()
                .mvcMatchers("/api/auth/status").permitAll()
                .mvcMatchers(HttpMethod.GET, "/login").permitAll()
                .mvcMatchers(HttpMethod.POST, "/api/profile").permitAll()
                .mvcMatchers("/health/**").permitAll()
                .mvcMatchers("/info/**").permitAll()
                .mvcMatchers("/h2-console/**").denyAll()
                .mvcMatchers("/api/admin/**").hasRole(ADMIN_ROLE)
                .anyRequest().fullyAuthenticated();

        http.sessionManagement()
                .sessionAuthenticationFailureHandler(authenticationFailureHandler);

        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);

        //TODO:
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        http.csrf().ignoringAntMatchers("/webauthn/**");


    }

}