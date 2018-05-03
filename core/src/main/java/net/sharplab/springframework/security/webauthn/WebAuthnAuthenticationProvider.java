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

package net.sharplab.springframework.security.webauthn;

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidator;
import net.sharplab.springframework.security.webauthn.authenticator.WebAuthnAuthenticatorService;
import net.sharplab.springframework.security.webauthn.exception.*;
import net.sharplab.springframework.security.webauthn.userdetails.WebAuthnUserDetails;
import net.sharplab.springframework.security.webauthn.userdetails.WebAuthnUserDetailsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.util.Base64Utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * WebAuthnAuthenticationProvider
 */
public class WebAuthnAuthenticationProvider implements AuthenticationProvider {

    protected final Log logger = LogFactory.getLog(getClass());

    //~ Instance fields
    // ================================================================================================
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private WebAuthnUserDetailsService userDetailsService;
    private WebAuthnAuthenticatorService authenticatorService;
    private WebAuthnAuthenticationContextValidator authenticationContextValidator;
    private boolean forcePrincipalAsString = false;
    private boolean hideCredentialIdNotFoundExceptions = true;
    private UserDetailsChecker preAuthenticationChecks = new DefaultPreAuthenticationChecks();
    private UserDetailsChecker postAuthenticationChecks = new DefaultPostAuthenticationChecks();
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public WebAuthnAuthenticationProvider(
            WebAuthnUserDetailsService webAuthnUserDetailsService,
            WebAuthnAuthenticatorService authenticatorService,
            WebAuthnAuthenticationContextValidator authenticationContextValidator) {
        this.userDetailsService = webAuthnUserDetailsService;
        this.authenticatorService = authenticatorService;
        this.authenticationContextValidator = authenticationContextValidator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only WebAuthnAssertionAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }

        WebAuthnAssertionAuthenticationToken authenticationToken = (WebAuthnAssertionAuthenticationToken) authentication;

        byte[] credentialId = Base64Utils.decodeFromUrlSafeString(authenticationToken.getCredentials().getCredentialId());

        // Using credential’s id attribute, look up the corresponding credential public key.
        Authenticator authenticator = retrieveWebAuthnAuthenticator(credentialId, authenticationToken);
        WebAuthnUserDetails user = userDetailsService.loadUserByAuthenticator(authenticator);

        preAuthenticationChecks.check(user);
        doAuthenticate(authenticationToken, authenticator, user);
        postAuthenticationChecks.check(user);

        Serializable principalToReturn = user;

        if (forcePrincipalAsString) {
            principalToReturn = user.getUsername();
        }

        WebAuthnAuthenticationToken result = new WebAuthnAuthenticationToken(
                principalToReturn, authenticationToken.getCredentials(),
                authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authenticationToken.getDetails());

        return result;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return WebAuthnAssertionAuthenticationToken.class.isAssignableFrom(authentication);
    }

    void doAuthenticate(WebAuthnAssertionAuthenticationToken authenticationToken, Authenticator authenticator, WebAuthnUserDetails user){
        if (authenticationToken.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "WebAuthnAuthenticationContextValidator.badCredentials",
                    "Bad credentials"));
        }


        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isUserVerified = Objects.equals(currentAuthentication.getName(), user.getUsername());
        boolean userVerificationRequired = !isUserVerified; // If user is not verified, user verification is required.

        try{
            authenticationContextValidator.validate(authenticationToken.getCredentials(), authenticator, userVerificationRequired);
        }
        catch (com.webauthn4j.validator.exception.MaliciousDataException e){
            throw new MaliciousDataException("Bad client data type", e);
        }
        catch (com.webauthn4j.validator.exception.BadChallengeException e){
            throw new BadChallengeException("Bad challenge", e);
        }
        catch (com.webauthn4j.validator.exception.BadOriginException e){
            throw new BadOriginException("Bad origin", e);
        }
        catch (com.webauthn4j.validator.exception.BadRpIdException e){
            throw new BadRpIdException("Bad rpId", e);
        }
        catch (com.webauthn4j.validator.exception.UserNotVerifiedException e){
            throw new UserNotVerifiedException("User not verified", e);
        }
        catch (com.webauthn4j.validator.exception.UserNotPresentException e){
            throw new UserNotPresentException("User not verified", e);
        }
        catch (com.webauthn4j.validator.exception.BadSignatureException e){
            throw new BadSignatureException("Bad signature", e);
        }
        catch (com.webauthn4j.validator.exception.MaliciousCounterValueException e){
            throw new MaliciousCounterValueException("Malicious counter value is detected. Cloned authenticators exist in parallel.", e);
        }


    }

    public void setForcePrincipalAsString(boolean forcePrincipalAsString) {
        this.forcePrincipalAsString = forcePrincipalAsString;
    }

    public boolean isForcePrincipalAsString() {
        return forcePrincipalAsString;
    }

    /**
     * By default the <code>WebAuthnAuthenticationProvider</code> throws a
     * <code>BadCredentialsException</code> if a credentialId is not found or the credential is
     * incorrect. Setting this property to <code>false</code> will cause
     * <code>CredentialIdNotFoundException</code>s to be thrown instead for the former. Note
     * this is considered less secure than throwing <code>BadCredentialsException</code>
     * for both exceptions.
     *
     * @param hideCredentialIdNotFoundExceptions set to <code>false</code> if you wish
     *                                           <code>CredentialIdNotFoundException</code>s to be thrown instead of the non-specific
     *                                           <code>BadCredentialsException</code> (defaults to <code>true</code>)
     */
    public void setHideCredentialIdNotFoundExceptions(boolean hideCredentialIdNotFoundExceptions) {
        this.hideCredentialIdNotFoundExceptions = hideCredentialIdNotFoundExceptions;
    }

    public void setAuthenticatorService(WebAuthnAuthenticatorService authenticatorService) {
        this.authenticatorService = authenticatorService;
    }


    protected WebAuthnAuthenticatorService getAuthenticatorService() {
        return authenticatorService;
    }

    Authenticator retrieveWebAuthnAuthenticator(byte[] credentialId, WebAuthnAssertionAuthenticationToken authenticationToken) {
        Authenticator loadedAuthenticator;

        try {
            loadedAuthenticator = this.getAuthenticatorService().loadWebAuthnAuthenticatorByCredentialId(credentialId);
        } catch (CredentialIdNotFoundException notFound) {
            if (hideCredentialIdNotFoundExceptions) {
                throw new BadCredentialsException(messages.getMessage(
                        "WebAuthnAuthenticationProvider.badCredentials",
                        "Bad credentials"));
            } else {
                throw notFound;
            }
        } catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedAuthenticator == null) {
            throw new InternalAuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedAuthenticator;

    }

    private class DefaultPreAuthenticationChecks implements UserDetailsChecker {
        @Override
        public void check(UserDetails user) {
            if (!user.isAccountNonLocked()) {
                logger.debug("User account is locked");

                throw new LockedException(messages.getMessage(
                        "WebAuthnAuthenticationProvider.locked",
                        "User account is locked"));
            }

            if (!user.isEnabled()) {
                logger.debug("User account is disabled");

                throw new DisabledException(messages.getMessage(
                        "WebAuthnAuthenticationProvider.disabled",
                        "User is disabled"));
            }

            if (!user.isAccountNonExpired()) {
                logger.debug("User account is expired");

                throw new AccountExpiredException(messages.getMessage(
                        "WebAuthnAuthenticationProvider.expired",
                        "User account has expired"));
            }
        }
    }

    private class DefaultPostAuthenticationChecks implements UserDetailsChecker {
        @Override
        public void check(UserDetails user) {
            if (!user.isCredentialsNonExpired()) {
                logger.debug("User account credentials have expired");

                throw new CredentialsExpiredException(messages.getMessage(
                        "WebAuthnAuthenticationProvider.credentialsExpired",
                        "User credentials have expired"));
            }
        }
    }
}