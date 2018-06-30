/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sharplab.springframework.security.webauthn.config.configurers;

import net.sharplab.springframework.security.webauthn.WebAuthnFirstOfMultiFactorDelegatingAuthenticationProvider;
import net.sharplab.springframework.security.webauthn.WebAuthnProcessingFilter;
import net.sharplab.springframework.security.webauthn.challenge.ChallengeRepository;
import net.sharplab.springframework.security.webauthn.challenge.HttpSessionChallengeRepository;
import net.sharplab.springframework.security.webauthn.parameter.ConditionEndpointFilter;
import net.sharplab.springframework.security.webauthn.parameter.ConditionProvider;
import net.sharplab.springframework.security.webauthn.parameter.ConditionProviderImpl;
import net.sharplab.springframework.security.webauthn.server.ServerPropertyProvider;
import net.sharplab.springframework.security.webauthn.server.ServerPropertyProviderImpl;
import net.sharplab.springframework.security.webauthn.userdetails.WebAuthnUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.MFATokenEvaluator;
import org.springframework.security.authentication.MFATokenEvaluatorImpl;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static net.sharplab.springframework.security.webauthn.WebAuthnProcessingFilter.*;

/**
 * Adds webAuthnLogin authentication. All attributes have reasonable defaults making all
 * parameters are optional. If no {@link #loginPage(String)} is specified, a default login
 * page will be generated by the framework.
 *
 * <h2>Security Filters</h2>
 * <p>
 * The following Filters are populated
 *
 * <ul>
 * <li>{@link WebAuthnProcessingFilter}</li>
 * <li>{@link ConditionEndpointFilter}</li>
 * </ul>
 *
 * <h2>Shared Objects Created</h2>
 * <p>
 * The following shared objects are populated
 * <ul>
 * <li>{@link ChallengeRepository}</li>
 * <li>{@link ConditionProvider}</li>
 * <li>{@link ServerPropertyProvider}</li>
 * </ul>
 *
 * <h2>Shared Objects Used</h2>
 * <p>
 * The following shared objects are used:
 *
 * <ul>
 * <li>{@link org.springframework.security.authentication.AuthenticationManager}</li>
 * <li>{@link MFATokenEvaluator}</li>
 * </ul>
 */
public final class WebAuthnLoginConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractAuthenticationFilterConfigurer<H, WebAuthnLoginConfigurer<H>, WebAuthnProcessingFilter> {

    //~ Instance fields
    // ================================================================================================
    private ChallengeRepository challengeRepository;
    private ConditionProvider conditionProvider;
    private ServerPropertyProvider serverPropertyProvider;

    public WebAuthnLoginConfigurer() {
        super(new WebAuthnProcessingFilter(), null);

        usernameParameter(SPRING_SECURITY_FORM_USERNAME_KEY);
        passwordParameter(SPRING_SECURITY_FORM_PASSWORD_KEY);
        credentialIdParameter(SPRING_SECURITY_FORM_CREDENTIAL_ID_KEY);
        clientDataParameter(SPRING_SECURITY_FORM_CLIENTDATA_KEY);
        authenticatorDataParameter(SPRING_SECURITY_FORM_AUTHENTICATOR_DATA_KEY);
        signatureParameter(SPRING_SECURITY_FORM_SIGNATURE_KEY);
        clientExtensionsJSONParameter(SPRING_SECURITY_FORM_CLIENT_EXTENSIONS_JSON_PARAMETER);
    }

    public static WebAuthnLoginConfigurer webAuthnLogin() {
        return new WebAuthnLoginConfigurer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(H http) throws Exception {
        super.init(http);

        ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);

        if(challengeRepository == null){
            String[] beanNames = applicationContext.getBeanNamesForType(ChallengeRepository.class);
            if(beanNames.length == 0){
                challengeRepository = new HttpSessionChallengeRepository();
            }
            else {
                challengeRepository = applicationContext.getBean(ChallengeRepository.class);
            }
        }
        http.setSharedObject(ChallengeRepository.class, challengeRepository);

        if(conditionProvider == null){
            WebAuthnUserDetailsService userDetailsService = applicationContext.getBean(WebAuthnUserDetailsService.class);
            conditionProvider = new ConditionProviderImpl(userDetailsService);
        }
        http.setSharedObject(ConditionProvider.class, conditionProvider);

        if(serverPropertyProvider == null){
            String[] beanNames = applicationContext.getBeanNamesForType(ServerPropertyProvider.class);
            if(beanNames.length == 0){
                serverPropertyProvider = new ServerPropertyProviderImpl(challengeRepository);
            }
            else {
                serverPropertyProvider = applicationContext.getBean(ServerPropertyProvider.class);
            }
        }
        http.setSharedObject(ServerPropertyProvider.class, serverPropertyProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(H http) throws Exception {
        super.configure(http);

        this.getAuthenticationFilter().setServerPropertyProvider(serverPropertyProvider);

        configureConditionEndpointFilter(http);
    }

    private void configureConditionEndpointFilter(H http) {
        ConditionEndpointFilter conditionEndpointFilter = new ConditionEndpointFilter(conditionProvider, serverPropertyProvider);

        MFATokenEvaluator mfaTokenEvaluator = http.getSharedObject(MFATokenEvaluator.class);
        if(mfaTokenEvaluator != null){
            conditionEndpointFilter.setMFATokenEvaluator(mfaTokenEvaluator);
        }

        AuthenticationTrustResolver trustResolver = http.getSharedObject(AuthenticationTrustResolver.class);
        if (trustResolver != null) {
            conditionEndpointFilter.setTrustResolver(trustResolver);
        }

        http.addFilterAfter(conditionEndpointFilter, SessionManagementFilter.class);
    }

    /**
     * The HTTP parameter to look for the username when performing authentication. Default
     * is "username".
     *
     * @param usernameParameter the HTTP parameter to look for the username when
     *                          performing authentication
     * @return the {@link FormLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> usernameParameter(String usernameParameter) {
        this.getAuthenticationFilter().setUsernameParameter(usernameParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the password when performing authentication. Default
     * is "password".
     *
     * @param passwordParameter the HTTP parameter to look for the password when
     *                          performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> passwordParameter(String passwordParameter) {
        getAuthenticationFilter().setPasswordParameter(passwordParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the credentialId when performing authentication. Default
     * is "credentialId".
     *
     * @param credentialIdParameter the HTTP parameter to look for the credentialId when
     *                              performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> credentialIdParameter(String credentialIdParameter) {
        this.getAuthenticationFilter().setCredentialIdParameter(credentialIdParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the clientData when performing authentication. Default
     * is "clientData".
     *
     * @param clientDataParameter the HTTP parameter to look for the clientData when
     *                            performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> clientDataParameter(String clientDataParameter) {
        this.getAuthenticationFilter().setClientDataParameter(clientDataParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the authenticatorData when performing authentication. Default
     * is "authenticatorData".
     *
     * @param authenticatorDataParameter the HTTP parameter to look for the authenticatorData when
     *                                   performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> authenticatorDataParameter(String authenticatorDataParameter) {
        this.getAuthenticationFilter().setAuthenticatorDataParameter(authenticatorDataParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the signature when performing authentication. Default
     * is "signature".
     *
     * @param signatureParameter the HTTP parameter to look for the signature when
     *                           performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> signatureParameter(String signatureParameter) {
        this.getAuthenticationFilter().setSignatureParameter(signatureParameter);
        return this;
    }

    /**
     * The HTTP parameter to look for the clientExtensionsJSON when performing authentication. Default
     * is "clientExtensionsJSON".
     *
     * @param clientExtensionsJSONParameter the HTTP parameter to look for the clientExtensionsJSON when
     *                                      performing authentication
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> clientExtensionsJSONParameter(String clientExtensionsJSONParameter) {
        this.getAuthenticationFilter().setClientExtensionsJSONParameter(clientExtensionsJSONParameter);
        return this;
    }

    /**
     * Forward Authentication Success Handler
     *
     * @param forwardUrl the target URL in case of success
     * @return he {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> successForwardUrl(String forwardUrl) {
        successHandler(new ForwardAuthenticationSuccessHandler(forwardUrl));
        return this;
    }

    /**
     * Forward Authentication Failure Handler
     *
     * @param forwardUrl the target URL in case of failure
     * @return he {@link WebAuthnLoginConfigurer} for additional customization
     */
    public WebAuthnLoginConfigurer<H> failureForwardUrl(String forwardUrl) {
        failureHandler(new ForwardAuthenticationFailureHandler(forwardUrl));
        return this;
    }

    /**
     * <p>
     * Specifies the URL to send users to if login is required. If used with
     * {@link WebSecurityConfigurerAdapter} a default login page will be generated when
     * this attribute is not specified.
     * </p>
     *
     * @param loginPage login page
     * @return the {@link WebAuthnLoginConfigurer} for additional customization
     */
    @Override
    public WebAuthnLoginConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }

    public WebAuthnLoginConfigurer<H> challengeRepository(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
        return this;
    }

    public WebAuthnLoginConfigurer<H> conditionProvider(ConditionProvider conditionProvider) {
        this.conditionProvider = conditionProvider;
        return this;
    }

    public WebAuthnLoginConfigurer<H> serverPropertyProvider(ServerPropertyProvider serverPropertyProvider) {
        this.serverPropertyProvider = serverPropertyProvider;
        return this;
    }



}
