package com.minilands.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.minilands.backend.config.GoogleProperties;
import com.minilands.backend.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Service
public class GoogleTokenVerifierService {

    private final GoogleProperties googleProperties;

    public GoogleTokenVerifierService(GoogleProperties googleProperties) {
        this.googleProperties = googleProperties;
    }

    public GoogleUserInfo verify(String idTokenString) {
        if (!StringUtils.hasText(googleProperties.getClientId())) {
            throw new IllegalArgumentException("Google client ID is not configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleProperties.getClientId()))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AuthException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new AuthException("Google email is not verified");
            }

            return new GoogleUserInfo(
                    payload.getSubject(),
                    email.toLowerCase(),
                    (String) payload.get("name"));
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Failed to verify Google token");
        }
    }
}
