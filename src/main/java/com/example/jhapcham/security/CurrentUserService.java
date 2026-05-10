package com.example.jhapcham.security;

import com.example.jhapcham.Error.AuthenticationException;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationException("Authentication is required");
        }
        return findByPrincipal(authentication.getName());
    }

    public User requireUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationException("Authentication is required");
        }
        return findByPrincipal(userDetails.getUsername());
    }

    public void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Admin access is required");
        }
    }

    public void requireSelfOrAdmin(User user, Long targetUserId) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (!user.getId().equals(targetUserId)) {
            throw new AuthorizationException("You do not have permission to access this resource");
        }
    }

    public void requireSellerSelfOrAdmin(User user, Long sellerUserId) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() != Role.SELLER || !user.getId().equals(sellerUserId)) {
            throw new AuthorizationException("Seller access is required");
        }
        if (user.getStatus() != Status.ACTIVE) {
            throw new AuthorizationException("Seller account is not approved");
        }
    }

    private User findByPrincipal(String principal) {
        User user = userRepository.findByUsernameOrEmail(principal, principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new AuthenticationException("Authenticated user no longer exists"));
        if (user.getStatus() == Status.BLOCKED) {
            throw new AuthorizationException("Account is blocked");
        }
        return user;
    }
}
