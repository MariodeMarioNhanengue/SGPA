package com.sgpa.mario.api.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite tokens de acesso (JWT HS256) com: sub=id, tipo, nome, roles. */
@Service
public class JwtService {

    public record TokenEmitido(String valor, long expiraEmSegundos) {}

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public TokenEmitido emitir(String tipo, Long id, String nome, List<String> roles) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(props.getExpiracaoMinutos(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("sgpa")
            .issuedAt(agora)
            .expiresAt(expira)
            .subject(String.valueOf(id))
            .claim("tipo", tipo)
            .claim("nome", nome)
            .claim("roles", roles)
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, props.getExpiracaoMinutos() * 60);
    }
}
