package com.yeniden.identity.auth;

import com.yeniden.identity.domain.User;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final TokenProperties properties;

    public AccessTokenIssuer(JwtEncoder encoder, TokenProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String issue(User user, AuthSession session, Instant issuedAt, Instant expiresAt) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) throw AuthException.unauthorized();
        var claims = JwtClaimsSet.builder().issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience())).subject(user.getId().toString())
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("sid", session.getId().toString())
                .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList());
        if (user.getNeighborhoodId() != null) claims.claim("nbh", user.getNeighborhoodId().toString());
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims.build())).getTokenValue();
    }
}
