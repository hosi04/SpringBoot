package com.example.identity_service.service;

import com.example.identity_service.dto.request.AuthenticatonRequest;
import com.example.identity_service.dto.request.IntrospectRequest;
import com.example.identity_service.dto.response.AuthenticationResponse;
import com.example.identity_service.dto.response.IntrospectResponse;
import com.example.identity_service.entity.User;
import com.example.identity_service.exception.AppException;
import com.example.identity_service.exception.ErrorCode;
import com.example.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository; // Dung cai nay de lay thong tin cua USER\
    @NonFinal
    @Value("${jwt.signerKey}") //Anotation nay dung de doc gia tri tu file yaml
    protected String SIGNER_KEY;

    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {
        var token = request.getToken();

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT singedJWT = SignedJWT.parse(token);

        //Kiem tra xem token het han hay chua?
        Date expiryTime = singedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = singedJWT.verify(verifier);

        return IntrospectResponse.builder()
                .valid(verified && expiryTime.after(new Date())) // .valid la bien cua IntrospectResponse
                .build();

    }
    public AuthenticationResponse authenticate (AuthenticatonRequest request){
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated =  passwordEncoder.matches(request.getPassword(),
                user.getPassword()); // Tham số đầu tiên là Passwoed do User nhập vào khi đăng nhập, thứ 2 là
        // Password của Username đó và đã được lưu vào DBMS
        if(!authenticated){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }
    private String generateToken(User user){

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        //Body
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername()) //Nhan biet client
                .issuer("hosi.com") // De dinh danh ai issuer token nay
                .issueTime(new Date()) // Thoi gian start, end token
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .claim("scope", buildScope(user)) // Mo rong
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); //Sign token
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // Function nay de lay ra het cac Role cua User va gan cho User do
    private String buildScope(User user){
        // USING StringJoiner TO CONNECT STRING TOGETHER
        // HERE ROLES AND PERMISSIONS OF USER WITH SPACE AS SEPARATORS
        StringJoiner stringJoiner = new StringJoiner(" ");

        // GET ROLES AND PERMISSIONS AND BUILD LIMIT CHAIN
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            // FOOP EACH ROLE
            user.getRoles().forEach(role -> {

                // IF USER HAVE ROLE ADD EACH ROLE TO StringJoiner WITH PREFIX ROLE_
                stringJoiner.add("ROLE_" + role.getName());

                // FOR EACH PERMISSION
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    // IF ROLE HAVE PERMISSIONS, ADD PERMISSION TO StringJoiner
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });
        }
        // RETURN ROLES AND PERMISSION CONNECT TOGETHER
        return stringJoiner.toString();
    }
}
