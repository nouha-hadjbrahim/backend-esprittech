package tn.esprit.backend_esprittech.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final ReferentielService referentielService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       UserService userService,
                       ReferentielService referentielService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.referentielService = referentielService;
    }

    @Transactional
    @Historise(action = ActionType.REGISTER, entityType = EntityType.USER, entityIdExpression = "#result.user().id()", newValuesExpression = "{'email': #request.email()}", summaryExpression = "'Inscription ' + #request.email()")
    public AuthResponse register(RegisterRequest request) {
        // 1. L'email/identifiant doit correspondre à une entrée du référentiel (source de vérité)
        ReferentielUtilisateur reference =
                referentielService.trouverCorrespondance(request.email(), request.identifiant());

        // 2. Unicité du compte applicatif
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet email");
        }
        if (userRepository.existsByIdentifiant(request.identifiant())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet identifiant");
        }

        // 3. Le rôle découle du type déclaré dans le référentiel
        TypeUtilisateur type = reference.getType();
        Role role = (type == TypeUtilisateur.ENSEIGNANT) ? Role.ROLE_ENSEIGNANT : Role.ROLE_ETUDIANT;

        User user = User.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .identifiant(request.identifiant())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .typeUtilisateur(type)
                .departement(reference.getDepartement())
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Historise(action = ActionType.LOGIN, entityType = EntityType.USER, entityIdExpression = "#result.user().id()", newValuesExpression = "{'email': #request.email()}", summaryExpression = "'Connexion ' + #request.email()")
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token invalide ou expiré");
        }
        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, userService.toEnrichedResponse(user));
    }
}
