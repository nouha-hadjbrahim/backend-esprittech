package tn.esprit.backend_esprittech.config.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import tn.esprit.backend_esprittech.config.CustomUserDetailsService;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final String TOKEN = "jwt-valide";

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private MessageChannel channel;

    private WebSocketAuthInterceptor interceptor;
    private User utilisateur;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(tokenProvider, userDetailsService);

        utilisateur = new User();
        utilisateur.setId(42L);
        utilisateur.setEmail("prof@esprit.tn");
        utilisateur.setPassword("secret");
        utilisateur.setRole(Role.ROLE_ENSEIGNANT);
    }

    @Test
    void preSend_validToken_shouldAttachPrincipalCarryingUserId() {
        StompHeaderAccessor accessor = connectAccessor("Bearer " + TOKEN);
        when(tokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(tokenProvider.isRefreshToken(TOKEN)).thenReturn(false);
        when(tokenProvider.getEmailFromToken(TOKEN)).thenReturn("prof@esprit.tn");
        when(userDetailsService.loadUserByUsername("prof@esprit.tn")).thenReturn(utilisateur);

        Message<?> message = messageWith(accessor);
        Message<?> result = interceptor.preSend(message, channel);

        assertSame(message, result, "le message doit etre transmis inchange");
        assertNotNull(accessor.getUser());
        assertEquals("42", accessor.getUser().getName());
    }

    @Test
    void preSend_refreshToken_shouldNotAuthenticate() {
        StompHeaderAccessor accessor = connectAccessor("Bearer " + TOKEN);
        when(tokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(tokenProvider.isRefreshToken(TOKEN)).thenReturn(true);

        interceptor.preSend(messageWith(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void preSend_invalidToken_shouldNotAuthenticate() {
        StompHeaderAccessor accessor = connectAccessor("Bearer " + TOKEN);
        when(tokenProvider.validateToken(TOKEN)).thenReturn(false);

        interceptor.preSend(messageWith(accessor), channel);

        assertNull(accessor.getUser());
        verify(tokenProvider, never()).getEmailFromToken(TOKEN);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void preSend_headerWithoutBearerPrefix_shouldNotAuthenticate() {
        StompHeaderAccessor accessor = connectAccessor("Basic dXNlcjpwYXNz");

        interceptor.preSend(messageWith(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(tokenProvider, userDetailsService);
    }

    @Test
    void preSend_withoutAuthorizationHeader_shouldNotAuthenticate() {
        StompHeaderAccessor accessor = connectAccessor(null);

        interceptor.preSend(messageWith(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(tokenProvider, userDetailsService);
    }

    @Test
    void preSend_nonConnectCommand_shouldPassThroughUntouched() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("Authorization", "Bearer " + TOKEN);
        accessor.setLeaveMutable(true);

        Message<?> message = messageWith(accessor);
        assertSame(message, interceptor.preSend(message, channel));

        assertNull(accessor.getUser());
        verifyNoInteractions(tokenProvider, userDetailsService);
    }

    @Test
    void preSend_messageWithoutStompAccessor_shouldPassThroughUntouched() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        assertSame(message, interceptor.preSend(message, channel));
        verifyNoInteractions(tokenProvider, userDetailsService);
    }

    private StompHeaderAccessor connectAccessor(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> messageWith(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
