package org.example.e_fashion.service.impl;

import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import org.example.e_fashion.dto.request.LoginRequestDTO;
import org.example.e_fashion.dto.request.RegisterRequestDTO;
import org.example.e_fashion.dto.request.UpdatePasswordDTO;
import org.example.e_fashion.dto.response.LoginResponseDTO;
import org.example.e_fashion.mapper.UserMapper;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.AuthService;
import org.example.e_fashion.service.EmailService;
import org.example.e_fashion.utils.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService, UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtils;
    private final EmailService emailService;

    @Override
    public void register(RegisterRequestDTO register) {
        log.info("New registration attempt for email: {}", register.getEmail());
        if(userRepository.existsByEmail(register.getEmail())){
            log.warn("Registration failed: Email {} already in use", register.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        try{
            UserEntity user = userMapper.toUserEntity(register);
            user.setPasswordHash(passwordEncoder.encode(register.getPassword()));
            user.setIsActive(false);

            UserEntity savedUser = userRepository.save(user);

            // tạo token verify account
            String token = jwtTokenUtils.generateVerifyToken(savedUser);

            String link =
                    "https://aitools.ptit.edu.vn/style/accept_account?token=" + token;

            emailService.sendVerifyAccountEmail(savedUser.getEmail(), link);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void acceptAccount(String token) {

        String email = jwtTokenUtils.extractUsername(token);

        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        if (!jwtTokenUtils.validateVerifyToken(token, user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token");
        }

        user.setIsActive(true);

        userRepository.save(user);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO login) {
        log.info("Login attempt for email: {}", login.getEmail());
        Optional<UserEntity> user = userRepository.findByEmailAndIsActive(login.getEmail(), true);
        if(user.isEmpty()){
            log.warn("Login failed: User {} not found or not active", login.getEmail());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + login.getEmail() + " not found");
        }
        UserEntity userEntity = user.get();
        if(!passwordEncoder.matches(login.getPassword(), userEntity.getPassword())){
            log.warn("Login failed: Wrong password for user {}", login.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
        }
        log.info("Login successful for user: {}", login.getEmail());

        String accessToken = jwtTokenUtils.generateAccessToken(userEntity);
        String refreshToken = jwtTokenUtils.generateRefreshToken(userEntity);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(userEntity.getRole())
                .build();
    }

    @Override
    public void updatePassword(UpdatePasswordDTO updatePassword) {
        Optional<UserEntity> user = userRepository.findByEmailAndIsActive(updatePassword.getEmail(), true);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + updatePassword.getEmail() + " not found");
        }
        UserEntity userEntity = user.get();
        if(!passwordEncoder.matches(updatePassword.getNewPassword(), userEntity.getPassword())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
        }
        userEntity.setPasswordHash(passwordEncoder.encode(updatePassword.getNewPassword()));
        userRepository.save(userEntity);
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository
                .findByEmailAndIsActive(email, true)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Email " + email + " not found"
                        )
                );
    }

    @Override
    public void forgotPassword(String email) throws MessagingException {

        UserEntity user = userRepository
                .findByEmailAndIsActive(email, true)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        // tạo token reset password (15 phút)
        String token = jwtTokenUtils.generateResetToken(user);

        String link =
                "http://localhost:5173/reset_password?token=" + token;

        emailService.sendResetPasswordEmail(user.getEmail(), link);
        System.out.println("Reset link: " + link);
    }

    @Override
    public void resetPassword(String token, String newPassword) {

        String email = jwtTokenUtils.extractUsername(token);

        UserEntity user = userRepository
                .findByEmailAndIsActive(email, true)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        if(!jwtTokenUtils.validateResetToken(token, user)){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }

//    @Override
//    public void acceptAccount(String token){
//        TokenRegisterEntity tokenRes = tokenRegisterRepositoty.findByToken(token);
//        if(tokenRes == null){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found");
//        }
//        if(tokenRes.getExpirationDate().before(new Date())){
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Token is expired");
//        }
//        UserEntity user = tokenRes.getUser();
//        user.setStatus(1);
//        userRepository.save(user);
//    }

//    @Override
//    public void resendToken(String email) {
//        Optional<UserEntity> user = userRepository.findByEmail(email);
//        if(user.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
//        }
//        UserEntity userEntity = user.get();
//        boolean valid = tokenRegisterRepositoty.existsValidTokenByUserId(
//                userEntity.getId(),
//                new java.sql.Date(System.currentTimeMillis()));
//        if(valid){
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The token has been sent, please check again.");
//        }
//        try{
//            String token = UUID.randomUUID().toString();
//            long oneDayInMillis = 24 * 60 * 60 * 1000;
//            String link = "<a href='https://vietmind.ai4life.com.vn/accept_account?token=" + token + "'>Tại đây</a>";
//            emailService.sendEmailWithToke(email, link);
//            TokenRegisterEntity tokenEntity = new TokenRegisterEntity();
//            tokenEntity.setToken(token);
//            tokenEntity.setUser(userEntity);
//            tokenEntity.setExpirationDate(new java.sql.Date(System.currentTimeMillis() + oneDayInMillis));
//            tokenRegisterRepositoty.save(tokenEntity);
//        }catch(Exception e){
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }
}