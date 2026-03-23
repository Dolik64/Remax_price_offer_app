package cz.stabuilder.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Srovnatelná nemovitost — buď aktuálně v nabídce, nebo nedávno prodaná.
 */
public class ComparableProperty {

    public enum Category {
        OFFER,
        SOLD
    }

    private Long id;
    private Category category = Category.OFFER;

    private String disposition;
    private int areaSqm;
    private String street;
    private String district;
    private String city = "Praha";
    private long priceKc;
    private String floor;
    private String extras;
    private String condition;

    private String soldDate;
    private String soldDuration;

    private List<String> photoFilenames = new ArrayList<>();
    private String descriptionText;
    private List<String> highlights = new ArrayList<>();
    private String brokerFeedback;

    public ComparableProperty() {
        this.id = System.currentTimeMillis();
    }

    // === Výpočty ===

    public long getPricePerSqm() {
        if (areaSqm <= 0) return 0;
        return Math.round((double) priceKc / areaSqm);
    }

    public String getFormattedPrice() {
        return String.format("%,d Kč", priceKc).replace(',', ' ');
    }

    public String getFormattedPricePerSqm() {
        long ppm = getPricePerSqm();
        return ppm > 0 ? String.format("%,d Kč", ppm).replace(',', ' ') : "–";
    }

    public String getTableSummary() {
        StringBuilder sb = new StringBuilder();
        if (floor != null && !floor.isBlank()) sb.append(floor);
        if (extras != null && !extras.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(extras);
        }
        if (category == Category.SOLD && soldDate != null && !soldDate.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Prodáno ").append(soldDate);
            if (soldDuration != null && !soldDuration.isBlank()) {
                sb.append(" za ").append(soldDuration);
            }
        }
        return sb.toString();
    }

    // === Aliasy pro PdfExportService ===

    /** Plocha jako String pro PDF šablonu. */
    public String getArea() {
        return String.valueOf(areaSqm);
    }

    /** Numerická cena pro PDF formátování. */
    public long getPriceNumeric() {
        return priceKc;
    }

    /** Cena za m² (alias). */
    public long getPricePerM2() {
        return getPricePerSqm();
    }

    /** Plná lokace (ulice). */
    public String getFullLocation() {
        return street != null ? street : "";
    }

    // === Getters & Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }

    public int getAreaSqm() { return areaSqm; }
    public void setAreaSqm(int areaSqm) { this.areaSqm = areaSqm; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public long getPriceKc() { return priceKc; }
    public void setPriceKc(long priceKc) { this.priceKc = priceKc; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getExtras() { return extras; }
    public void setExtras(String extras) { this.extras = extras; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getSoldDate() { return soldDate; }
    public void setSoldDate(String soldDate) { this.soldDate = soldDate; }

    public String getSoldDuration() { return soldDuration; }
    public void setSoldDuration(String soldDuration) { this.soldDuration = soldDuration; }

    public List<String> getPhotoFilenames() { return photoFilenames; }
    public void setPhotoFilenames(List<String> photoFilenames) { this.photoFilenames = photoFilenames; }

    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String descriptionText) { this.descriptionText = descriptionText; }

    public List<String> getHighlights() { return highlights; }
    public void setHighlights(List<String> highlights) { this.highlights = highlights; }

    public String getBrokerFeedback() { return brokerFeedback; }
    public void setBrokerFeedback(String brokerFeedback) { this.brokerFeedback = brokerFeedback; }
}