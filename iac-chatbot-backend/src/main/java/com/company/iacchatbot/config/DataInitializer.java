package com.company.iacchatbot.config;

import com.company.iacchatbot.model.Role;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialise les données de test au démarrage de l'application
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Créer l'utilisateur admin si il n'existe pas
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@iacchatbot.com");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            // Quotas généreux pour l'administrateur (UC-13)
            admin.setQuotaCpu(128);
            admin.setQuotaRam(512);
            admin.setQuotaStorage(5000);
            userRepository.save(admin);
            logger.info("✅ Utilisateur ADMIN créé: admin / password123");
        }

        // Créer l'utilisateur user si il n'existe pas
        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setEmail("user@iacchatbot.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(Role.USER);
            user.setEnabled(true);
            // Quotas par défaut (UC-13)
            user.setQuotaCpu(32);
            user.setQuotaRam(128);
            user.setQuotaStorage(1000);
            userRepository.save(user);
            logger.info("✅ Utilisateur USER créé: user / password123");
        }

        logger.info("🎯 Initialisation des données terminée!");
    }
}
