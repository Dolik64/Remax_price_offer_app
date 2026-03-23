package cz.stabuilder.controller;

import cz.stabuilder.model.StaProject;
import cz.stabuilder.service.DocxExportService;
import cz.stabuilder.service.PdfExportService;
import cz.stabuilder.service.PhotoService;
import cz.stabuilder.service.ProjectService;
import cz.stabuilder.service.StaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * REST API pro STA Builder.
 */
@RestController
@RequestMapping("/api")
public class StaController {

    private final StaService staService;
    private final DocxExportService docxExportService;
    private final PdfExportService pdfExportService;
    private final PhotoService photoService;
    private final ProjectService projectService;

    public StaController(StaService staService,
                         DocxExportService docxExportService,
                         PdfExportService pdfExportService,
                         PhotoService photoService,
                         ProjectService projectService) {
        this.staService = staService;
        this.docxExportService = docxExportService;
        this.pdfExportService = pdfExportService;
        this.photoService = photoService;
        this.projectService = projectService;
    }

    // ==================== PROJEKTY (save/load/list/delete) ====================

    @PostMapping("/projects")
    public Map<String, String> saveProject(@RequestBody StaProject project) throws IOException {
        projectService.save(project);
        return Map.of("status", "ok", "name", project.getProjectName());
    }

    @GetMapping("/projects")
    public List<String> listProjects() throws IOException {
        return projectService.listProjects();
    }

    @GetMapping("/projects/{name}")
    public ResponseEntity<StaProject> loadProject(@PathVariable String name) {
        try {
            StaProject project = projectService.load(name);
            return ResponseEntity.ok(project);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/projects/{name}")
    public Map<String, String> deleteProject(@PathVariable String name) throws IOException {
        boolean deleted = projectService.delete(name);
        return Map.of("status", deleted ? "ok" : "not_found");
    }

    // ==================== FOTKY ====================

    @PostMapping("/photos")
    public Map<String, String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        try {
            String filename = photoService.savePhoto(file);
            return Map.of("status", "ok", "filename", filename);
        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/photos/batch")
    public Map<String, Object> uploadPhotos(@RequestParam("files") List<MultipartFile> files) {
        List<String> filenames = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            try {
                filenames.add(photoService.savePhoto(file));
            } catch (IOException e) {
                errors.add(e.getMessage());
            }
        }
        return Map.of("status", "ok", "filenames", filenames, "errors", errors);
    }

    @GetMapping("/photos/{filename}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String filename) {
        try {
            var path = photoService.getPhotoPath(filename);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            byte[] data = Files.readAllBytes(path);
            String ct = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== EXPORT PDF ====================

    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody StaProject project) throws IOException {
        byte[] pdf = pdfExportService.generatePdf(project);
        String filename = "STA_" + safeName(project.getSubject().getClientName()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/export/pdf/preview")
    public ResponseEntity<byte[]> previewPdf(@RequestBody StaProject project) throws IOException {
        byte[] pdf = pdfExportService.generatePdf(project);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ==================== EXPORT DOCX ====================

    @GetMapping("/export/docx")
    public ResponseEntity<byte[]> exportDocx() throws IOException {
        byte[] docx = docxExportService.generateDocx(staService.getProject());
        String filename = "STA_" + safeName(staService.getSubject().getClientName()) + ".docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docx);
    }

    @GetMapping("/export/json")
    public StaProject exportJson() {
        return staService.getProject();
    }

    // ==================== LEGACY (stará API z StaService) ====================

    @GetMapping("/project")
    public StaProject getProject() {
        return staService.getProject();
    }

    @PostMapping("/project/reset")
    public Map<String, String> resetProject() {
        staService.resetProject();
        return Map.of("status", "ok");
    }

    // === Helper ===

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "export";
        return name.replaceAll("[^a-zA-Z0-9ěščřžýáíéůúďťňĚŠČŘŽÝÁÍÉŮÚĎŤŇ]", "_");
    }
}