package cz.stabuilder.service;

import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.Pricing;
import cz.stabuilder.model.StaProject;
import cz.stabuilder.model.Subject;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generuje DOCX soubor Srovnávací tržní analýzy z dat projektu.
 */
@Service
public class DocxExportService {

    private final StaService staService;

    public DocxExportService(StaService staService) {
        this.staService = staService;
    }

    /**
     * Vygeneruje DOCX a vrátí jako byte pole.
     */
    public byte[] generateDocx(StaProject project) throws IOException {
        XWPFDocument doc = new XWPFDocument();

        // --- Titulní strana ---
        addTitle(doc, "SROVNÁVACÍ TRŽNÍ ANALÝZA");
        addText(doc, project.getSubject().getDate().toString(), false);
        addText(doc, "Vypracováno pro: " + project.getSubject().getClientName(), true);
        addText(doc, String.format("Předmět prodeje: byt %s, %s, %s m²%s",
                project.getSubject().getDisposition(),
                project.getSubject().getOwnership(),
                project.getSubject().getAreaSqm(),
                project.getSubject().getExtras() != null ? " + " + project.getSubject().getExtras() : ""),
                true);
        addText(doc, "Adresa: " + project.getSubject().getAddress(), true);

        // --- Popis nemovitosti ---
        addHeading(doc, "Popis nemovitosti", 2);
        addText(doc, project.getSubject().getDescription(), false);

        // --- Fotky předmětu ---
        addSubjectPhotos(doc, project.getSubject());

        // --- Srovnatelné nemovitosti v nabídce ---
        List<ComparableProperty> offers = project.getOffers();
        if (!offers.isEmpty()) {
            addHeading(doc, "Podobné nabídky aktuálně v prodeji", 1);
            for (ComparableProperty comp : offers) {
                addComparableSection(doc, comp);
            }
        }

        // --- Nedávno prodané ---
        List<ComparableProperty> sold = project.getSold();
        if (!sold.isEmpty()) {
            addHeading(doc, "Nedávno prodané nemovitosti", 1);
            for (ComparableProperty comp : sold) {
                addComparableSection(doc, comp);
            }
        }

        // --- Srovnávací tabulka ---
        addHeading(doc, "Nabízené / prodané byty — srovnání", 1);
        addComparisonTable(doc, project);

        // --- Cenové doporučení ---
        addHeading(doc, "Návrh pásma prodejní ceny", 1);
        addPricingSection(doc, project.getPricing());

        // Export
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    // === Pomocné metody ===

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(200);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(22);
        run.setFontFamily("Arial");
    }

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading" + level);
        p.setSpacingBefore(300);
        p.setSpacingAfter(150);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(level == 1 ? 16 : 14);
        run.setFontFamily("Arial");
    }

    private void addText(XWPFDocument doc, String text, boolean bold) {
        if (text == null || text.isBlank()) return;
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(80);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(11);
        run.setFontFamily("Arial");
    }

    private void addSubjectPhotos(XWPFDocument doc, Subject subject) {
        if (subject.getPhotoFilenames().isEmpty()) return;

        addHeading(doc, "Fotky pracovní", 2);

        for (String filename : subject.getPhotoFilenames()) {
            addPhoto(doc, filename);
        }
    }

    private void addComparableSection(XWPFDocument doc, ComparableProperty comp) {
        // Nadpis: dispozice, plocha, ulice, městská část
        String heading = String.format("Byt %s, %d m², ul. %s, Praha — %s",
                comp.getDisposition(), comp.getAreaSqm(),
                comp.getStreet(), comp.getDistrict());
        addHeading(doc, heading, 2);

        // Cena
        String priceText = comp.getFormattedPrice();
        if (comp.getCategory() == ComparableProperty.Category.SOLD) {
            priceText = "Prodáno za " + priceText;
            if (comp.getSoldDate() != null) priceText += " v " + comp.getSoldDate();
            if (comp.getSoldDuration() != null) priceText += " za " + comp.getSoldDuration();
        }
        addText(doc, priceText, true);

        // Fotky
        for (String filename : comp.getPhotoFilenames()) {
            addPhoto(doc, filename);
        }

        // Popis
        if (comp.getDescriptionText() != null && !comp.getDescriptionText().isBlank()) {
            addText(doc, comp.getDescriptionText(), false);
        }

        // Highlights — TODO: implementovat zvýraznění v textu
        if (!comp.getHighlights().isEmpty()) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingBefore(100);
            XWPFRun label = p.createRun();
            label.setText("Důležité body: ");
            label.setBold(true);
            label.setFontSize(11);
            label.setFontFamily("Arial");
            XWPFRun vals = p.createRun();
            vals.setText(String.join(", ", comp.getHighlights()));
            vals.setFontSize(11);
            vals.setFontFamily("Arial");
        }

        // Makléř
        if (comp.getBrokerFeedback() != null && !comp.getBrokerFeedback().isBlank()) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingBefore(100);
            XWPFRun label = p.createRun();
            label.setText("Zpětná vazba od makléře: ");
            label.setBold(true);
            label.setUnderline(UnderlinePatterns.SINGLE);
            label.setFontSize(11);
            label.setFontFamily("Arial");
            XWPFRun text = p.createRun();
            text.setText(comp.getBrokerFeedback());
            text.setFontSize(11);
            text.setFontFamily("Arial");
        }
    }

    private void addPhoto(XWPFDocument doc, String filename) {
        Path photoPath = staService.getPhotoPath(filename);
        if (!photoPath.toFile().exists()) return;

        try {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = p.createRun();

            String lower = filename.toLowerCase();
            int picType;
            if (lower.endsWith(".png")) picType = XWPFDocument.PICTURE_TYPE_PNG;
            else if (lower.endsWith(".gif")) picType = XWPFDocument.PICTURE_TYPE_GIF;
            else picType = XWPFDocument.PICTURE_TYPE_JPEG;

            try (FileInputStream fis = new FileInputStream(photoPath.toFile())) {
                run.addPicture(fis, picType, filename,
                        Units.toEMU(200), Units.toEMU(150)); // šířka x výška v bodech
            }
        } catch (Exception e) {
            // Pokud fotku nelze vložit, přeskočíme
            addText(doc, "[Fotka: " + filename + " — chyba při vkládání]", false);
        }
    }

    private void addComparisonTable(XWPFDocument doc, StaProject project) {
        List<ComparableProperty> all = project.getComparables();
        if (all.isEmpty()) {
            addText(doc, "Žádné srovnatelné nemovitosti.", false);
            return;
        }

        List<ComparableProperty> offers = project.getOffers();
        List<ComparableProperty> sold = project.getSold();

        // Počet řádků: header + (nabídka header?) + offers + (prodáno header?) + sold
        int totalRows = 1; // header
        if (!offers.isEmpty()) totalRows += 1 + offers.size();
        if (!sold.isEmpty()) totalRows += 1 + sold.size();

        XWPFTable table = doc.createTable(totalRows, 4);
        table.setWidth("100%");

        // Header
        setCell(table, 0, 0, "Lokalita", true);
        setCell(table, 0, 1, "Prodejní cena", true);
        setCell(table, 0, 2, "Cena za m²", true);
        setCell(table, 0, 3, "Výhody / Nevýhody", true);

        int row = 1;

        if (!offers.isEmpty()) {
            setCellSpan(table, row, "Nabídka", 4);
            row++;
            for (ComparableProperty c : offers) {
                setCell(table, row, 0, c.getStreet() + "\n" + c.getDisposition() + ", " + c.getAreaSqm() + " m²", false);
                setCell(table, row, 1, c.getFormattedPrice(), false);
                setCell(table, row, 2, c.getFormattedPricePerSqm(), false);
                setCell(table, row, 3, c.getTableSummary(), false);
                row++;
            }
        }

        if (!sold.isEmpty()) {
            setCellSpan(table, row, "Prodáno", 4);
            row++;
            for (ComparableProperty c : sold) {
                setCell(table, row, 0, c.getStreet() + "\n" + c.getDisposition() + ", " + c.getAreaSqm() + " m²", false);
                setCell(table, row, 1, c.getFormattedPrice(), false);
                setCell(table, row, 2, c.getFormattedPricePerSqm(), false);
                setCell(table, row, 3, c.getTableSummary(), false);
                row++;
            }
        }
    }

    private void setCell(XWPFTable table, int row, int col, String text, boolean bold) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) p = cell.addParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(10);
        run.setFontFamily("Arial");
    }

    private void setCellSpan(XWPFTable table, int row, String text, int span) {
        XWPFTableRow tableRow = table.getRow(row);
        XWPFTableCell firstCell = tableRow.getCell(0);
        XWPFParagraph p = firstCell.getParagraphArray(0);
        if (p == null) p = firstCell.addParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(10);
        run.setFontFamily("Arial");

        // Merge cells
        CTTcPr tcPr = firstCell.getCTTc().addNewTcPr();
        tcPr.addNewGridSpan().setVal(java.math.BigInteger.valueOf(span));

        for (int i = 1; i < span; i++) {
            XWPFTableCell mergedCell = tableRow.getCell(i);
            if (mergedCell != null) {
                CTTcPr pr = mergedCell.getCTTc().addNewTcPr();
                pr.addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    private void addPricingSection(XWPFDocument doc, Pricing pricing) {
        // Klady
        if (pricing.getPositives() != null && !pricing.getPositives().isBlank()) {
            addText(doc, "Kladné stránky nemovitosti:", true);
            for (String line : pricing.getPositives().split("\n")) {
                if (!line.isBlank()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(360);
                    XWPFRun run = p.createRun();
                    run.setText("• " + line.trim());
                    run.setFontSize(11);
                    run.setFontFamily("Arial");
                }
            }
        }

        // Zápory
        if (pricing.getNegatives() != null && !pricing.getNegatives().isBlank()) {
            addText(doc, "Záporné stránky nemovitosti:", true);
            for (String line : pricing.getNegatives().split("\n")) {
                if (!line.isBlank()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(360);
                    XWPFRun run = p.createRun();
                    run.setText("• " + line.trim());
                    run.setFontSize(11);
                    run.setFontFamily("Arial");
                }
            }
        }

        // Doporučení
        if (pricing.getRecommendation() != null && !pricing.getRecommendation().isBlank()) {
            addText(doc, "Doporučení:", true);
            addText(doc, pricing.getRecommendation(), false);
        }

        // Cenové rozmezí
        if (pricing.getPriceFrom() > 0 && pricing.getPriceTo() > 0) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingBefore(200);
            XWPFRun run = p.createRun();
            run.setText("Doporučené cenové rozmezí: " + pricing.getFormattedRange());
            run.setBold(true);
            run.setFontSize(12);
            run.setFontFamily("Arial");
        }

        // Počáteční cena
        if (pricing.getStartPrice() > 0) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingBefore(100);
            XWPFRun run = p.createRun();
            run.setText("Počáteční prodejní cena: " + pricing.getFormattedStartPrice());
            run.setBold(true);
            run.setUnderline(UnderlinePatterns.SINGLE);
            run.setFontSize(12);
            run.setFontFamily("Arial");
        }
    }
}
