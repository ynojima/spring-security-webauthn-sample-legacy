package net.sharplab.springframework.security.webauthn.context.provider;

import net.sharplab.springframework.security.webauthn.client.Origin;
import net.sharplab.springframework.security.webauthn.attestation.authenticator.WebAuthnAuthenticatorData;
import net.sharplab.springframework.security.webauthn.client.ClientData;
import net.sharplab.springframework.security.webauthn.client.challenge.Challenge;
import net.sharplab.springframework.security.webauthn.client.challenge.DefaultChallenge;
import net.sharplab.springframework.security.webauthn.client.challenge.HttpSessionChallengeRepository;
import net.sharplab.springframework.security.webauthn.context.RelyingParty;
import net.sharplab.springframework.security.webauthn.context.WebAuthnAuthenticationContext;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.Base64Utils;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for WebAuthnAuthenticationContextProviderImpl
 */
public class WebAuthnAuthenticationContextProviderImplTest {

    @Ignore
    @Test
    public void provide_test() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "http://localhost:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String credentialId = "StWIWIe1Fg2hAuPemrFVw9JmvK65xn6okTw5bR5p9K4M58NdExovuNezAn2ToqvbEtSUbIHPVvKoXCE7-PjRy5QNhncuYkn9pKvbM00E5I0";
        String clientData = "eyJjaGFsbGVuZ2UiOiJ4V2ozRWRxMlM2YVlyd1FMWHJtR3JBIiwiaGFzaEFsZyI6IlNIQS0yNTYiLCJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjgwODAifQ";
        String authenticatorData = "SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MBAAAAGg";
        Challenge savedChallenge = new DefaultChallenge(new byte[]{0x00});
        Origin origin = new Origin("http", "localhost", 8080);
        String rpId = "localhost";
        String signature = "MEUCIQC42cvnjdgqMVYGPXzf8-CaU4RYPEMgxkzgmFFwn1oC4QIgfrLsvf5WPexdVWBsNckVE3RnXTrzAMX75EgMkpjjQ1I";
        Authentication currentAuthentication = new AnonymousAuthenticationToken("dummyKey", null, null);

        Origin expectedOrigin = new Origin("http", "localhost", 8080);

        RelyingPartyProvider relyingPartyProvider = mock(RelyingPartyProvider.class);
        when(relyingPartyProvider.provide(any(), any())).thenReturn(new RelyingParty(origin, rpId, savedChallenge));
        WebAuthnAuthenticationContextProviderImpl webAuthnContextProvider = new WebAuthnAuthenticationContextProviderImpl(relyingPartyProvider);
        WebAuthnAuthenticationContext context = webAuthnContextProvider.provide(request, response, credentialId, clientData, authenticatorData, signature, currentAuthentication);
        assertThat(context).isNotNull();
        assertThat(context.getCredentialId()).isEqualTo(credentialId);
        assertThat(context.getRawClientData()).isEqualTo(Base64Utils.decodeFromUrlSafeString(clientData));
        assertThat(context.getClientDataJson()).isEqualTo("{\"challenge\":\"xWj3Edq2S6aYrwQLXrmGrA\",\"hashAlg\":\"SHA-256\",\"origin\":\"http://localhost:8080\"}");
        assertThat(context.getClientData()).isInstanceOf(ClientData.class);
        assertThat(context.getRawAuthenticatorData()).isEqualTo(Base64Utils.decodeFromUrlSafeString(authenticatorData));
        assertThat(context.getAuthenticatorData()).isInstanceOf(WebAuthnAuthenticatorData.class);
        assertThat(context.getSignature()).isEqualTo(Base64Utils.decodeFromUrlSafeString(signature));
        assertThat(context.getRelyingParty().getChallenge().getValue()).isEqualTo(new byte[]{0x00});
        assertThat(context.getRelyingParty().getOrigin()).isEqualTo(expectedOrigin);
        assertThat(context.getRelyingParty().getRpId()).isEqualTo("localhost");
    }

    @Ignore
    @Test
    public void provide_test_with_custom_rpId() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "http://localhost:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String credentialId = "StWIWIe1Fg2hAuPemrFVw9JmvK65xn6okTw5bR5p9K4M58NdExovuNezAn2ToqvbEtSUbIHPVvKoXCE7-PjRy5QNhncuYkn9pKvbM00E5I0";
        String clientData = "eyJjaGFsbGVuZ2UiOiJ4V2ozRWRxMlM2YVlyd1FMWHJtR3JBIiwiaGFzaEFsZyI6IlNIQS0yNTYiLCJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjgwODAifQ";
        String authenticatorData = "SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MBAAAAGg";
        String signature = "MEUCIQC42cvnjdgqMVYGPXzf8-CaU4RYPEMgxkzgmFFwn1oC4QIgfrLsvf5WPexdVWBsNckVE3RnXTrzAMX75EgMkpjjQ1I";
        Authentication currentAuthentication = new AnonymousAuthenticationToken("dummyKey", null, null);

        RelyingPartyProviderImpl relyingPartyProvider = new RelyingPartyProviderImpl(new HttpSessionChallengeRepository());
        WebAuthnAuthenticationContextProviderImpl webAuthnContextProvider = new WebAuthnAuthenticationContextProviderImpl(relyingPartyProvider);
        assertThat(relyingPartyProvider.getRpId()).isNull();
        relyingPartyProvider.setRpId("example.com");
        WebAuthnAuthenticationContext context = webAuthnContextProvider.provide(request, response, credentialId, clientData, authenticatorData, signature, currentAuthentication);
        assertThat(context.getRelyingParty().getRpId()).isEqualTo("example.com");
    }

}
