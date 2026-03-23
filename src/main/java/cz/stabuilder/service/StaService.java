package cz.stabuilder.service;

import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.Pricing;
import cz.stabuilder.model.StaProject;
import cz.stabuilder.model.Subject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Služba pro správu STA projektu.
 * Drží aktuální projekt v paměti (single-user app).
 * Fotky ukládá na disk do uploads/.
 */
@Service
public class StaService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    private StaProject project = new StaProject();

    public StaService() {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Nelze vytvořit adresář pro uploady", e);
        }
    }

    // --- Projekt ---

    public StaProject getProject() {
        return project;
    }

    public void resetProject() {
        project = new StaProject();
    }

    // --- Předmět prodeje ---

    public Subject getSubject() {
        return project.getSubject();
    }

    public void updateSubject(Subject updated) {
        // Zachová existující fotky pokud nové nejsou dodány
        if (updated.getPhotoFilenames() == null || updated.getPhotoFilenames().isEmpty()) {
            updated.setPhotoFilenames(project.getSubject().getPhotoFilenames());
        }
        project.setSubject(updated);
    }

    // --- Srovnatelné nemovitosti ---

    public ComparableProperty addComparable() {
        ComparableProperty comp = new ComparableProperty();
        project.addComparable(comp);
        return comp;
    }

    public ComparableProperty updateComparable(Long id, ComparableProperty updated) {
        ComparableProperty existing = project.findComparable(id);
        if (existing == null) return null;

        existing.setCategory(updated.getCategory());
        existing.setDisposition(updated.getDisposition());
        existing.setAreaSqm(updated.getAreaSqm());
        existing.setStreet(updated.getStreet());
        existing.setDistrict(updated.getDistrict());
        existing.setPriceKc(updated.getPriceKc());
        existing.setFloor(updated.getFloor());
        existing.setExtras(updated.getExtras());
        existing.setSoldDate(updated.getSoldDate());
        existing.setSoldDuration(updated.getSoldDuration());
        existing.setDescriptionText(updated.getDescriptionText());
        existing.setHighlights(updated.getHighlights());
        existing.setBrokerFeedback(updated.getBrokerFeedback());

        // Fotky se mění jen přes upload endpoint
        return existing;
    }

    public boolean removeComparable(Long id) {
        return project.removeComparable(id);
    }

    // --- Cenové doporučení ---

    public Pricing getPricing() {
        return project.getPricing();
    }

    public void updatePricing(Pricing updated) {
        project.setPricing(updated);
    }

    // --- Fotky ---

    /**
     * Uloží nahraný soubor a vrátí jeho unikátní název.
     */
    public String savePhoto(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String uniqueName = UUID.randomUUID().toString().substring(0, 8) + ext;

        Path target = UPLOAD_DIR.resolve(uniqueName);
        file.transferTo(target.toFile());

        return uniqueName;
    }

    /**
     * Vrátí cestu k fotce.
     */
    public Path getPhotoPath(String filename) {
        return UPLOAD_DIR.resolve(filename);
    }

    /**
     * Přidá fotku k předmětu prodeje.
     */
    public void addSubjectPhoto(String filename) {
        if (project.getSubject().getPhotoFilenames().size() < 6) {
            project.getSubject().getPhotoFilenames().add(filename);
        }
    }

    /**
     * Odstraní fotku z předmětu prodeje.
     */
    public void removeSubjectPhoto(int index) {
        var photos = project.getSubject().getPhotoFilenames();
        if (index >= 0 && index < photos.size()) {
            photos.remove(index);
        }
    }

    /**
     * Přidá fotku ke srovnatelné nemovitosti.
     */
    public void addComparablePhoto(Long compId, String filename) {
        ComparableProperty comp = project.findComparable(compId);
        if (comp != null && comp.getPhotoFilenames().size() < 6) {
            comp.getPhotoFilenames().add(filename);
        }
    }

    /**
     * Odstraní fotku ze srovnatelné nemovitosti.
     */
    public void removeComparablePhoto(Long compId, int index) {
        ComparableProperty comp = project.findComparable(compId);
        if (comp != null) {
            var photos = comp.getPhotoFilenames();
            if (index >= 0 && index < photos.size()) {
                photos.remove(index);
            }
        }
    }
}
