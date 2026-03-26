package org.example.company.tcs.techcellshop.security;

import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailUserIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        String role = user.getRoleUser() == null ? "USER" : user.getRoleUser().toUpperCase(Locale.ROOT);

        return new org.springframework.security.core.userdetails.User(
          user.getEmailUser(),
          user.getPasswordUser(),
          List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

}
