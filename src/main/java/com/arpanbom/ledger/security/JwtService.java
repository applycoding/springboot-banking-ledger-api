package com.arpanbom.ledger.security;

import com.arpanbom.ledger.config.LedgerProperties;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ISSUER = "banking-ledger";

    private final JwtEncoder encoder;
    private final long expiresInSeconds;

    public JwtService(JwtEncoder encoder, LedgerProperties properties) {
        this.encoder = encoder;
        this.expiresInSeconds = properties.jwt().expiration().toSeconds();
    }

    public String issue(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .subject(username)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return expiresInSeconds;
    }
}
