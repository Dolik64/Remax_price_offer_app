package cz.stabuilder.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Předmět prodeje — nemovitost, pro kterou se STA připravuje.
 */
public class Subject {

    private LocalDate date;
    private String clientName;
    private String disposition;
    private String ownership;
    private int areaSqm;
    private String extras;
    private String address;
    private String description;
    private List<String> photoFilenames = new ArrayList<>();

    public Subject() {
        this.date = LocalDate.now();
        this.ownership = "OV";
    }

    /** Plocha jako String pro PDF šablonu. */
    public String getArea() {
        return String.valueOf(areaSqm);
    }

    // --- Getters & Setters ---

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }

    public String getOwnership() { return ownership; }
    public void setOwnership(String ownership) { this.ownership = ownership; }

    public int getAreaSqm() { return areaSqm; }
    public void setAreaSqm(int areaSqm) { this.areaSqm = areaSqm; }

    public String getExtras() { return extras; }
    public void setExtras(String extras) { this.extras = extras; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getPhotoFilenames() { return photoFilenames; }
    public void setPhotoFilenames(List<String> photoFilenames) { this.photoFilenames = photoFilenames; }
}