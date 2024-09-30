package com.example.identity_service.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.identity_service.dto.request.AuthenticatonRequest;
import com.example.identity_service.dto.request.IntrospectRequest;
import com.example.identity_service.dto.request.LogoutRequest;
import com.example.identity_service.dto.request.RefreshRequest;
import com.example.identity_service.dto.response.AuthenticationResponse;
import com.example.identity_service.dto.response.IntrospectResponse;
import com.example.identity_service.entity.InvalidatedToken;
import com.example.identity_service.entity.User;
import com.example.identity_service.exception.AppException;
import com.example.identity_service.exception.ErrorCode;
import com.example.identity_service.repository.InvalidatedTokenRepository;
import com.example.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    UserRepository userRepository; // Dung cai nay de lay thong tin cua USER
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}") // Anotation nay dung de doc gia tri tu file yaml
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected Long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected Long REFRESHABLE_DURATION;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) { // Bắt lỗi đã định nghĩa của Appexception
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid) // .valid la bien cua IntrospectResponse
                .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT singedJWT = SignedJWT.parse(token);
        // Kiem tra xem token het han hay chua?
        Date expiryTime = (isRefresh)
                ? new Date(singedJWT
                        .getJWTClaimsSet()
                        .getIssueTime() // getIssueTime là thời gian hết hạn của 1 token
                        .toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : singedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = singedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) // Nếu Token này không hợp lệ hoặc đã hết hạn
        throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Kiểm tra xem token này đã logout hay chưa
        if (invalidatedTokenRepository.existsById(singedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED); // Sẽ gây ra lỗi để lên hàm Introspect bắt lỗi

        return singedJWT; // Trả về 1 cái token
    }

    public AuthenticationResponse authenticate(AuthenticatonRequest request) { // Dùng trong API Get Token
        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()); // Tham số đầu tiên là Passwoed do User nhập vào khi đăng nhập, thứ 2 là
        // Password của Username đó và đã được lưu vào DBMS
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    private String generateToken(User user) {

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        // Body
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername()) // Nhan biet client, thêm tên User vào trong token
                .issuer("hosi.com") // De dinh danh ai issuer token nay
                .issueTime(new Date()) // Thoi gian start, end token
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString()) // Random jwtID
                .claim("scope", buildScope(user)) // Mo rong
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); // Sign token
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // Function nay de lay ra het cac Role cua User va gan cho User do
    private String buildScope(User user) {
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

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true); // Function verifyToken trả về 1 Token

            // Đọc các thông tin của token
            String jid = signToken.getJWTClaimsSet().getJWTID(); // Lấy ID của token
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime(); // Lấy thời gian hết hạn của token
            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jid).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken); // Lưu vào CSDL
        } catch (AppException e) {
            log.info("Token already exprired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException { // Trả về 1 token và hiệu lực mới
        var signedJwt =
                verifyToken(request.getToken(), true); // TRUE là mình muốn dùng hàm verifyRoken với mục đích là refresh
        var jit = signedJwt.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJwt.getJWTClaimsSet().getExpirationTime();
        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
        invalidatedTokenRepository.save(invalidatedToken); // Logout token cũ

        var username =
                signedJwt.getJWTClaimsSet().getSubject(); // Do ở hàm generateToken ta lưu tên Username trong subject
        var user =
                userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // GenerateToken dựa vào thông tin user
        var token = generateToken(user);
        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }
}
