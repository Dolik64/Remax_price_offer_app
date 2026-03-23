package cz.stabuilder.model;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * Celý STA projekt — sdružuje předmět prodeje, srovnatelné nemovitosti a cenové doporučení.
 */
public class StaProject {

    private String projectName;
    private Subject subject = new Subject();
    private List<ComparableProperty> comparables = new ArrayList<>();
    private Pricing pricing = new Pricing();

    // Agent info
    private String agentName = "Martin Halgaš";
    private String agentTitle = "Realitní makléř";
    private String agentPhone = "+420 731 502 750";
    private String agentEmail = "martin.halgas@re-max.cz";
    private String agencyName = "RE/MAX Anděl";
    private String agencyAddress = "Ostrovského 253/3 – 150 00 Praha 5 - Smíchov";
    private String agentWebsite = "www.halgasreality.cz";

    public StaProject() {}

    // === Název projektu ===

    public String getProjectName() {
        if (projectName != null && !projectName.isBlank()) {
            return projectName;
        }
        StringBuilder sb = new StringBuilder("STA");
        if (subject.getClientName() != null && !subject.getClientName().isBlank()) {
            sb.append("_").append(subject.getClientName().trim());
        }
        if (subject.getAddress() != null && !subject.getAddress().isBlank()) {
            String street = subject.getAddress().split(",")[0].trim();
            sb.append("_").append(street);
        }
        return sb.toString();
    }

    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getFilenameSafe() {
        return getProjectName()
                .replaceAll("[^a-zA-Z0-9ěščřžýáíéůúďťňĚŠČŘŽÝÁÍÉŮÚĎŤŇ_\\-]", "_")
                .replaceAll("_+", "_");
    }

    // === Filtrování ===

    public List<ComparableProperty> getOffers() {
        return comparables.stream()
                .filter(c -> c.getCategory() == ComparableProperty.Category.OFFER)
                .toList();
    }

    public List<ComparableProperty> getSold() {
        return comparables.stream()
                .filter(c -> c.getCategory() == ComparableProperty.Category.SOLD)
                .toList();
    }

    // === CRUD ===

    public void addComparable(ComparableProperty comp) { comparables.add(comp); }

    public boolean removeComparable(Long id) {
        return comparables.removeIf(c -> c.getId().equals(id));
    }

    public ComparableProperty findComparable(Long id) {
        return comparables.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    // === Statistiky ===

    public int getTotalPhotos() {
        int count = subject.getPhotoFilenames().size();
        for (ComparableProperty c : comparables) count += c.getPhotoFilenames().size();
        return count;
    }

    public int getTotalComparables() { return comparables.size(); }

    public OptionalDouble getAveragePricePerSqm() {
        return comparables.stream().filter(c -> c.getPricePerSqm() > 0)
                .mapToLong(ComparableProperty::getPricePerSqm).average();
    }

    public boolean isReadyForExport() {
        return subject.getClientName() != null && !subject.getClientName().isBlank()
                && subject.getAddress() != null && !subject.getAddress().isBlank()
                && !comparables.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("StaProject[name=%s, client=%s, comparables=%d, photos=%d]",
                getProjectName(), subject.getClientName(), comparables.size(), getTotalPhotos());
    }

    // === Getters & Setters ===

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public List<ComparableProperty> getComparables() { return comparables; }
    public void setComparables(List<ComparableProperty> comparables) { this.comparables = comparables; }

    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentTitle() { return agentTitle; }
    public void setAgentTitle(String agentTitle) { this.agentTitle = agentTitle; }

    public String getAgentPhone() { return agentPhone; }
    public void setAgentPhone(String agentPhone) { this.agentPhone = agentPhone; }

    public String getAgentEmail() { return agentEmail; }
    public void setAgentEmail(String agentEmail) { this.agentEmail = agentEmail; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public String getAgencyAddress() { return agencyAddress; }
    public void setAgencyAddress(String agencyAddress) { this.agencyAddress = agencyAddress; }

    public String getAgentWebsite() { return agentWebsite; }
    public void setAgentWebsite(String agentWebsite) { this.agentWebsite = agentWebsite; }
}