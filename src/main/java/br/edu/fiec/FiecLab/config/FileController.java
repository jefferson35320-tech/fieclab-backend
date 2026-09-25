package br.edu.fiec.FiecLab.config;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/images")
public class FileController {

    private final Path uploadDir = Paths.get("uploads");

    @GetMapping("/{fileName}")
    public ResponseEntity getImage(@PathVariable String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Descobre o Content-Type (image/jpeg, image/png, etc.)
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            // Lê os bytes do arquivo no disco
            byte[] imageBytes = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}