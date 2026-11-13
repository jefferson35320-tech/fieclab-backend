package br.edu.fiec.FiecLab.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    // Conteúdo bruto do JSON, injetado via Kubernetes secret -> env var FIREBASE_CREDENTIALS_JSON
    @Value("${app.firebase.credentials-json:}")
    private String credentialsJson;

    // Fallback local (dev): arquivo já existente em resources
    @Value("${app.firebase.config-file:firebase-service-account.json}")
    private String fallbackConfigFile;

    // Caminho onde o JSON vindo do secret será materializado em disco
    @Value("${app.firebase.generated-file-path:/tmp/firebase-service-account.json}")
    private String generatedFilePath;

    @PostConstruct
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream credentials = resolveCredentialsStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }

    private InputStream resolveCredentialsStream() throws IOException {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            Path filePath = Paths.get(generatedFilePath);
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, credentialsJson, StandardCharsets.UTF_8);
            return Files.newInputStream(filePath);
        }
        return new ClassPathResource(fallbackConfigFile).getInputStream();
    }
}
