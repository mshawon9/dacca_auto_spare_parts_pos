package com.daccaauto.pos.service;

import com.daccaauto.pos.exception.ProductImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    public static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final Path storageDirectory;

    public ProductImageStorageService(@Value("${app.product-image-directory:uploads/product-images}") String directory) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public StoredImage store(MultipartFile image) {
        validate(image);

        String contentType = image.getContentType();
        String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        Path target = storageDirectory.resolve(fileName).normalize();

        if (!target.getParent().equals(storageDirectory)) {
            throw new ProductImageException("Invalid product image filename");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredImage(fileName, contentType);
        } catch (IOException ex) {
            throw new ProductImageException("Could not save product image");
        }
    }

    public byte[] load(String fileName) {
        Path path = storageDirectory.resolve(fileName).normalize();
        if (!path.getParent().equals(storageDirectory) || !Files.exists(path)) {
            throw new ProductImageException("Product image not found");
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new ProductImageException("Could not load product image");
        }
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Path path = storageDirectory.resolve(fileName).normalize();
        if (!path.getParent().equals(storageDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ProductImageException("Could not delete product image");
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ProductImageException("Please select an image");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ProductImageException("Product image must not exceed 2 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new ProductImageException("Only JPEG and PNG product images are allowed");
        }

        try (InputStream inputStream = image.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new ProductImageException("Selected file is not a valid image");
            }
        } catch (IOException ex) {
            throw new ProductImageException("Could not validate product image");
        }
    }

    public record StoredImage(String fileName, String contentType) {
    }
}
