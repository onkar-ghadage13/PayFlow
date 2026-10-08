package com.playflow.config;

import com.playflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final String adminEmail;

    public AdminBootstrap(UserRepository users, @Value("${app.admin.email:}") String adminEmail) {
        this.users = users;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) return;
        users.findByEmailIgnoreCase(adminEmail).ifPresent(user -> {
            if (!"ADMIN".equals(user.role)) {
                user.role = "ADMIN";
                users.save(user);
            }
        });
    }
}
