package com.wrenchlog.wrenchlog.security;

import com.wrenchlog.wrenchlog.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_setsAuthentication_whenCookieValid() throws Exception {
        Cookie authCookie = new Cookie("auth_token", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{authCookie});

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("alice");
        when(jwtService.parseClaims("valid-token")).thenReturn(claims);

        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_skipsAuthentication_whenNoCookiePresent() throws Exception {
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_skipsAuthentication_whenTokenExpiredOrInvalid() throws Exception {
        Cookie authCookie = new Cookie("auth_token", "expired-token");
        when(request.getCookies()).thenReturn(new Cookie[]{authCookie});
        when(jwtService.parseClaims("expired-token")).thenThrow(new JwtException("expired"));

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_findsAuthTokenCookie_amongOtherCookies() throws Exception {
        Cookie otherCookie = new Cookie("some_other_cookie", "irrelevant");
        Cookie authCookie = new Cookie("auth_token", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, authCookie});

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("alice");
        when(jwtService.parseClaims("valid-token")).thenReturn(claims);

        User user = new User("alice", "alice@test.com", "hashed");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}