package tn.esprit.backend_esprittech.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.service.EmailVerificationService.PendingData;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AzureGraphService azureGraphService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       UserService userService,
                       AzureGraphService azureGraphService,
                       EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.azureGraphService = azureGraphService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Étape 1 de l'inscription : valide l'appartenance à l'organisation, puis
     * envoie un code de vérification à l'email. Aucun compte n'est encore créé.
     */
    @Transactional
    public void startRegistration(RegisterRequest request) {
        // 1. Unicité du compte applicatif (avant l'appel réseau à Azure)
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet email");
        }

        // 2. L'email doit appartenir à l'organisation ESPRIT (Azure Entra ID) ;
        //    le rôle découle de l'appartenance aux groupes enseignant / étudiant.
        AzureGraphService.GraphMember member = azureGraphService.resolveMember(request.email());

        // 3. Enregistre l'inscription en attente et envoie le code OTP par email.
        emailVerificationService.startVerification(new PendingData(
                request.email(),
                request.nom(),
                request.prenom(),
                passwordEncoder.encode(request.password()),
                member.type(),
                member.departement()));
    }

    /**
     * Étape 2 de l'inscription : valide le code OTP, crée le compte et connecte
     * l'utilisateur (dépôt des tokens).
     */
    @Transactional
    public AuthResponse verifyEmailAndCreate(String email, String code) {
        PendingData data = emailVerificationService.consumeVerifiedRegistration(email, code);

        // Garde-fou contre une inscription concurrente sur le même email.
        if (userRepository.existsByEmail(data.email())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet email");
        }

        Role role = (data.type() == TypeUtilisateur.ENSEIGNANT)
                ? Role.ROLE_ENSEIGNANT : Role.ROLE_ETUDIANT;

        User user = User.builder()
                .nom(data.nom())
                .prenom(data.prenom())
                .email(data.email())
                .password(data.encodedPassword())
                .role(role)
                .typeUtilisateur(data.type())
                .departement(data.departement())
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    /** Renvoie un nouveau code de vérification pour une inscription en attente. */
    public void resendVerificationCode(String email) {
        emailVerificationService.resendCode(email);
    }

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
