package org.example.e_fashion.service;

import jakarta.mail.MessagingException;
import org.example.e_fashion.dto.request.LoginRequestDTO;
import org.example.e_fashion.dto.request.RegisterRequestDTO;
import org.example.e_fashion.dto.request.UpdatePasswordDTO;
import org.example.e_fashion.dto.response.LoginResponseDTO;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    void register(RegisterRequestDTO register);

    void acceptAccount(String token);

    LoginResponseDTO login(LoginRequestDTO login);

    void updatePassword(UpdatePasswordDTO updatePassword);

    UserDetails loadUserByUsername(String email);

    void forgotPassword(String email) throws MessagingException;

    void resetPassword(String token, String newPassword);
}
