package tn.esprit.backend_esprittech.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validToken_setsAuthentication() throws Exception {
        User user = buildUser();
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid-token")).thenReturn(false);
        when(tokenProvider.getEmailFromToken("valid-token")).thenReturn("test@esprit.tn");
        when(userDetailsService.loadUserByUsername("test@esprit.tn")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername("test@esprit.tn");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic credentials"})
    void doFilter_noResolvableToken_continuesWithoutAuth(String headerValue) throws Exception {
        when(request.getHeader("Authorization")).thenReturn(headerValue);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilter_invalidToken_continuesWithoutAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(tokenProvider.validateToken("bad")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilter_refreshToken_rejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer refresh-tok");
        when(tokenProvider.validateToken("refresh-tok")).thenReturn(true);
        when(tokenProvider.isRefreshToken("refresh-tok")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilter_alreadyAuthenticated_skipsTokenProcessing() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null, List.of()));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    private User buildUser() {
        return User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build();
    }
}
