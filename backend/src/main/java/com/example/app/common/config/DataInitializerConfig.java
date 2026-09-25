package com.example.app.common.config;

import com.example.app.features.auth.domain.entity.Role;
import com.example.app.features.auth.domain.entity.User;
import com.example.app.features.auth.domain.repository.UserRepository;
import com.example.app.features.notifications.domain.entity.Notification;
import com.example.app.features.notifications.domain.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Value("${app.seed.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${app.seed.user.email:user@example.com}")
    private String userEmail;

    @Value("${app.seed.user.password:User1234!}")
    private String userPassword;

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // 1. Initialiser le Super Admin
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .firstName("Super")
                        .lastName("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build();

                User savedAdmin = userRepository.save(admin);
                log.info("Compte Administrateur initialisé avec succès : {}", adminEmail);

                // Notification de bienvenue pour l'admin
                notificationRepository.save(Notification.builder()
                        .recipientId(savedAdmin.getId())
                        .title("Bienvenue Super Admin")
                        .message("La plateforme et la base de données sont prêtes pour l'épreuve.")
                        .type("SUCCESS")
                        .read(false)
                        .build());
            }

            // 2. Initialiser un Utilisateur standard de test
            if (!userRepository.existsByEmail(userEmail)) {
                User standardUser = User.builder()
                        .firstName("Test")
                        .lastName("User")
                        .email(userEmail)
                        .password(passwordEncoder.encode(userPassword))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build();

                User savedUser = userRepository.save(standardUser);
                log.info("Compte Utilisateur de test initialisé avec succès : {}", userEmail);

                notificationRepository.save(Notification.builder()
                        .recipientId(savedUser.getId())
                        .title("Notification de bienvenue")
                        .message("Bienvenue sur la plateforme ! Votre compte utilisateur standard est actif.")
                        .type("INFO")
                        .read(false)
                        .build());
            }
        };
    }
}
