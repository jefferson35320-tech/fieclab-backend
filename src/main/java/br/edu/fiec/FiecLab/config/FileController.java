package br.edu.fiec.FiecLab.config;

import br.edu.fiec.FiecLab.Features.Produts.Services.S3StorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/images")
public class FileController {

    private final S3StorageService storage;

    public FileController(S3StorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) {
        try {
            var obj = storage.download(fileName);

            String contentType = obj.response().contentType() != null
                    ? obj.response().contentType()
                    : MediaType.IMAGE_JPEG_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .body(obj.asByteArray());

        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }
}