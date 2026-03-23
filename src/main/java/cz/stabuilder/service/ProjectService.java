package cz.stabuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.stabuilder.model.StaProject;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ProjectService {

    private static final String DATA_DIR = "data/projects";
    private final ObjectMapper mapper;

    public ProjectService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(DATA_DIR));
    }

    /**
     * Uložit projekt do JSON souboru.
     */
    public void save(StaProject project) throws IOException {
        String filename = sanitizeFilename(project.getProjectName());
        if (filename.isBlank()) {
            filename = "projekt_" + System.currentTimeMillis();
        }
        Path path = Path.of(DATA_DIR, filename + ".json");
        mapper.writeValue(path.toFile(), project);
    }

    /**
     * Načíst projekt ze souboru.
     */
    public StaProject load(String name) throws IOException {
        Path path = Path.of(DATA_DIR, name + ".json");
        if (!Files.exists(path)) {
            throw new IOException("Projekt nenalezen: " + name);
        }
        return mapper.readValue(path.toFile(), StaProject.class);
    }

    /**
     * Seznam všech uložených projektů.
     */
    public List<String> listProjects() throws IOException {
        List<String> projects = new ArrayList<>();
        Path dir = Path.of(DATA_DIR);
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                      .forEach(p -> {
                          String name = p.getFileName().toString();
                          projects.add(name.substring(0, name.length() - 5));
                      });
            }
        }
        return projects;
    }

    /**
     * Smazat projekt.
     */
    public boolean delete(String name) throws IOException {
        Path path = Path.of(DATA_DIR, name + ".json");
        return Files.deleteIfExists(path);
    }

    /**
     * Serialize project to JSON string (for API responses).
     */
    public String toJson(StaProject project) throws IOException {
        return mapper.writeValueAsString(project);
    }

    /**
     * Deserialize project from JSON string.
     */
    public StaProject fromJson(String json) throws IOException {
        return mapper.readValue(json, StaProject.class);
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9áčďéěíňóřšťúůýžÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ _-]", "")
                    .replaceAll("\\s+", "_")
                    .toLowerCase();
    }
}
