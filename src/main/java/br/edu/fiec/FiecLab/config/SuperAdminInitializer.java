package br.edu.fiec.FiecLab.config;

import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.UserStatus;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Garante um SUPER_ADMIN ao iniciar a aplicação, a partir de SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD.
 * - Conta já existe: é promovida a SUPER_ADMIN (a senha não é alterada).
 * - Conta não existe: é criada com SUPER_ADMIN_PASSWORD (obrigatória nesse caso).
 * Sem SUPER_ADMIN_EMAIL, nada é feito.
 */
@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public SuperAdminInitializer(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.super-admin.email:}") String email,
                                 @Value("${app.super-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty()) {
            log.info("SUPER_ADMIN_EMAIL não definido; nenhum SUPER_ADMIN foi configurado.");
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if (user.getPosition() != Role.SUADMIN) {
                user.setPosition(Role.SUADMIN);
                userRepository.save(user);
                log.info("Usuário {} promovido a SUPER_ADMIN.", email);
            }
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn("SUPER_ADMIN_EMAIL={} não existe e SUPER_ADMIN_PASSWORD não foi definido; conta não criada.", email);
            return;
        }

        User admin = new User();
        admin.setUuid(UUID.randomUUID().toString());
        admin.setName("Super Admin");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPosition(Role.SUADMIN);
        admin.setStatus(UserStatus.CONFIRMED);
        userRepository.save(admin);
        log.info("SUPER_ADMIN {} criado.", email);
    }
}
