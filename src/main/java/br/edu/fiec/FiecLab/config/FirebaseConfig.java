package br.edu.fiec.FiecLab.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct; // Spring Boot 3+. Para Spring Boot 2 use javax.annotation.PostConstruct
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    // Variável de ambiente injetada pelo Kubernetes Secret (conteúdo JSON do service account)
    @Value("${FIREBASE_CREDENTIALS}")
    private String firebaseCredentials;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp já inicializado, ignorando.");
            return;
        }

        Path credentialsFile = null;
        try {
            // 1 e 2 - Converte o conteúdo da variável de ambiente em um arquivo JSON temporário
            // (createTempFile já cria com permissão 600 em sistemas POSIX)
            credentialsFile = Files.createTempFile("firebase-credentials-", ".json");
            Files.writeString(credentialsFile, firebaseCredentials, StandardCharsets.UTF_8);

            // 3 - Usa o arquivo nas configurações do Firebase
            try (InputStream serviceAccount = Files.newInputStream(credentialsFile)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp inicializado com sucesso.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao inicializar o Firebase", e);
        } finally {
            // Remove o arquivo com credenciais assim que a inicialização termina
            if (credentialsFile != null) {
                try {
                    Files.deleteIfExists(credentialsFile);
                } catch (IOException e) {
                    log.warn("Não foi possível remover o arquivo temporário de credenciais.", e);
                }
            }
        }
    }
}