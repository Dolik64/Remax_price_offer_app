package cz.stabuilder.model;

import java.util.List;

/**
 * Cenové doporučení — klady, zápory, rozmezí a počáteční cena.
 */
public class Pricing {

    private String positives;       // kladné stránky (řádky oddělené \n)
    private String negatives;       // záporné stránky
    private String recommendation;  // textové doporučení
    private long priceFrom;         // cenové rozmezí OD
    private long priceTo;           // cenové rozmezí DO
    private long startPrice;        // navržená počáteční cena

    public Pricing() {}

    // === Formátování ===

    public String getFormattedPriceFrom() {
        return priceFrom > 0 ? String.format("%,d Kč", priceFrom).replace(',', ' ') : "";
    }

    public String getFormattedPriceTo() {
        return priceTo > 0 ? String.format("%,d Kč", priceTo).replace(',', ' ') : "";
    }

    public String getFormattedStartPrice() {
        return startPrice > 0 ? String.format("%,d Kč", startPrice).replace(',', ' ') : "";
    }

    public String getFormattedRange() {
        if (priceFrom > 0 && priceTo > 0) {
            return String.format("od %s do %s", getFormattedPriceFrom(), getFormattedPriceTo());
        }
        return "";
    }

    // === Aliasy pro PdfExportService ===

    /** Formátované cenové rozmezí pro PDF. */
    public String getPriceRangeFormatted() {
        if (priceFrom > 0 && priceTo > 0) {
            return getFormattedPriceFrom() + " do " + getFormattedPriceTo();
        }
        return "";
    }

    /** Formátovaná počáteční cena (alias). */
    public String getStartPriceFormatted() {
        return getFormattedStartPrice();
    }

    /** Kladné stránky jako List (pro PDF iteraci). */
    public List<String> getPositivesList() {
        if (positives == null || positives.isBlank()) return List.of();
        return List.of(positives.split("\n")).stream()
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
    }

    /** Záporné stránky jako List (pro PDF iteraci). */
    public List<String> getNegativesList() {
        if (negatives == null || negatives.isBlank()) return List.of();
        return List.of(negatives.split("\n")).stream()
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
    }

    // --- Getters & Setters ---

    public String getPositives() { return positives; }
    public void setPositives(String positives) { this.positives = positives; }

    public String getNegatives() { return negatives; }
    public void setNegatives(String negatives) { this.negatives = negatives; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public long getPriceFrom() { return priceFrom; }
    public void setPriceFrom(long priceFrom) { this.priceFrom = priceFrom; }

    public long getPriceTo() { return priceTo; }
    public void setPriceTo(long priceTo) { this.priceTo = priceTo; }

    public long getStartPrice() { return startPrice; }
    public void setStartPrice(long startPrice) { this.startPrice = startPrice; }
}