package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.config.AuthCookieFactory;
import tn.esprit.backend_esprittech.dto.request.ChangePasswordRequest;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.request.ResendCodeRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateProfileRequest;
import tn.esprit.backend_esprittech.dto.request.VerifyEmailRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.dto.response.MessageResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.service.AuthService;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.Arrays;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, refresh et profil courant")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthCookieFactory cookieFactory;

    public AuthController(AuthService authService,
                          UserService userService,
                          AuthCookieFactory cookieFactory) {
        this.authService = authService;
        this.userService = userService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription — étape 1 : envoi du code",
            description = "Vérifie l'appartenance à l'organisation ESPRIT (Azure) et envoie un code "
                    + "de vérification à l'email. Aucun compte n'est créé à cette étape.")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.startRegistration(request);
        return ResponseEntity.accepted().body(MessageResponse.of(
                "Un code de vérification a été envoyé à votre adresse email."));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Inscription — étape 2 : vérification du code",
            description = "Valide le code OTP reçu par email, crée le compte et dépose les tokens "
                    + "dans des cookies HttpOnly.")
    public ResponseEntity<UserResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                     HttpServletResponse response) {
        AuthResponse result = authService.verifyEmailAndCreate(request.email(), request.code());
        writeAuthCookies(response, result);
        return ResponseEntity.status(HttpStatus.CREATED).body(result.user());
    }

    @PostMapping("/register/resend")
    @Operation(summary = "Inscription — renvoyer le code",
            description = "Renvoie un nouveau code de vérification pour une inscription en attente.")
    public ResponseEntity<MessageResponse> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendVerificationCode(request.email());
        return ResponseEntity.accepted().body(MessageResponse.of(
                "Un nouveau code de vérification a été envoyé à votre adresse email."));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion",
            description = "Authentifie l'utilisateur et dépose access + refresh tokens dans des "
                    + "cookies HttpOnly. Le corps ne contient que les infos utilisateur.")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse result = authService.login(request);
        writeAuthCookies(response, result);
        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token",
            description = "Lit le refresh token depuis le cookie HttpOnly et redépose un nouveau "
                    + "couple de cookies.")
    public ResponseEntity<UserResponse> refresh(HttpServletRequest request,
                                               HttpServletResponse response) {
        String refreshToken = readCookie(request, AuthCookieFactory.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null) {
            throw new BadCredentialsException("Refresh token manquant");
        }
        AuthResponse result = authService.refresh(refreshToken);
        writeAuthCookies(response, result);
        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion",
            description = "Efface les cookies access + refresh côté navigateur.")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Profil courant", description = "Retourne l'utilisateur authentifié, enrichi de son affiliation.")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getCurrentUserResponse());
    }

    @PutMapping("/me")
    @Operation(summary = "Modifier mon profil",
            description = "Met à jour le nom et le prénom de l'utilisateur authentifié (email et rôle non modifiables).")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateOwnProfile(request);
        return ResponseEntity.ok(userService.getCurrentUserResponse());
    }

    @PutMapping("/me/password")
    @Operation(summary = "Changer mon mot de passe",
            description = "Change le mot de passe de l'utilisateur authentifié après vérification de l'ancien.")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /** Dépose l'access et le refresh token dans deux cookies HttpOnly distincts. */
    private void writeAuthCookies(HttpServletResponse response, AuthResponse result) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.accessToken(result.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.refreshToken(result.refreshToken()).toString());
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + result.accessToken());
    }

    /** Lit la valeur (non vide) d'un cookie par son nom, ou {@code null} s'il est absent. */
    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
