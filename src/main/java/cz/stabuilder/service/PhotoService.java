package cz.stabuilder.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class PhotoService {

    private static final String UPLOAD_DIR = "data/photos";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(UPLOAD_DIR));
    }

    /**
     * Uložit nahraný soubor a vrátit jeho unikátní název.
     */
    public String savePhoto(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Soubor je prázdný");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Soubor je příliš velký (max 10 MB)");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!isAllowedExtension(extension)) {
            throw new IOException("Nepodporovaný formát souboru: " + extension);
        }

        String uniqueName = UUID.randomUUID().toString().substring(0, 12) + "." + extension;
        Path targetPath = Path.of(UPLOAD_DIR, uniqueName);
        file.transferTo(targetPath);

        return uniqueName;
    }

    /**
     * Získat cestu k fotce.
     */
    public Path getPhotoPath(String filename) {
        return Path.of(UPLOAD_DIR, filename);
    }

    /**
     * Ověřit existenci fotky.
     */
    public boolean exists(String filename) {
        return Files.exists(Path.of(UPLOAD_DIR, filename));
    }

    /**
     * Smazat fotku.
     */
    public boolean deletePhoto(String filename) throws IOException {
        return Files.deleteIfExists(Path.of(UPLOAD_DIR, filename));
    }

    /**
     * Získat byte[] fotky pro PDF export.
     */
    public byte[] getPhotoBytes(String filename) throws IOException {
        Path path = Path.of(UPLOAD_DIR, filename);
        if (!Files.exists(path)) {
            throw new IOException("Fotka nenalezena: " + filename);
        }
        return Files.readAllBytes(path);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String ext) {
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")
                || ext.equals("webp") || ext.equals("gif");
    }
}
