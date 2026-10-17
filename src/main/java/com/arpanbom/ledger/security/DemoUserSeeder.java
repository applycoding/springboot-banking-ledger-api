package com.arpanbom.ledger.security;

import com.arpanbom.ledger.auth.AppUser;
import com.arpanbom.ledger.auth.AppUserRepository;
import com.arpanbom.ledger.auth.DemoUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByUsername(DemoUsers.USERNAME).isEmpty()) {
            users.save(AppUser.create(DemoUsers.USERNAME, passwordEncoder.encode(DemoUsers.PASSWORD)));
            log.info("Demo operator user '{}' is ready", DemoUsers.USERNAME);
        }
    }
}
