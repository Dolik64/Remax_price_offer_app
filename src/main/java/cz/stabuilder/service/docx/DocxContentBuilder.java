package cz.stabuilder.service.docx;

import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.Pricing;
import cz.stabuilder.model.StaProject;
import cz.stabuilder.model.Subject;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static cz.stabuilder.service.docx.DocxStyleHelper.*;

/**
 * Generuje obsahové sekce DOCX: titulní stranu, srovnatelné nemovitosti,
 * cenové doporučení a patičku s kontaktem.
 */
public class DocxContentBuilder {

    private final DocxPhotoHelper photoHelper;

    public DocxContentBuilder(DocxPhotoHelper photoHelper) {
        this.photoHelper = photoHelper;
    }

    // ==================== TITLE PAGE ====================

    public void addTitlePage(XWPFDocument doc, StaProject project) {
        Subject subject = project.getSubject();

        addEmptyLines(doc, 2);

        // Hlavní nadpis
        XWPFParagraph titleP = doc.createParagraph();
        titleP.setAlignment(ParagraphAlignment.CENTER);
        titleP.setSpacingAfter(400);
        addStyledRun(titleP, "SROVNÁVACÍ TRŽNÍ ANALÝZA", 28, true, COLOR_PRIMARY);

        // Datum
        if (subject.getDate() != null) {
            XWPFParagraph dateP = doc.createParagraph();
            dateP.setAlignment(ParagraphAlignment.CENTER);
            dateP.setSpacingAfter(400);
            addStyledRun(dateP, subject.getDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")), 14, true, null);
        }

        // Klient
        addLabelValue(doc, "Vypracováno pro: ", safe(subject.getClientName()), 12);

        // Předmět prodeje
        addLabelValue(doc, "Předmět prodeje: ", buildSubjectLine(subject), 11);

        // Adresa
        addLabelValue(doc, "Adresa: ", safe(subject.getAddress()), 11);

        addEmptyLines(doc, 1);

        // Popis nemovitosti
        if (subject.getDescription() != null && !subject.getDescription().isBlank()) {
            addSubHeading(doc, "Popis nemovitosti:");
            for (String line : subject.getDescription().split("\n")) {
                if (!line.isBlank()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setSpacingAfter(60);
                    addStyledRun(p, line.trim(), 10, false, COLOR_GRAY);
                }
            }
        }
    }

    // ==================== SUBJECT PHOTOS ====================

    public void addSubjectPhotosPage(XWPFDocument doc, Subject subject) {
        addSubHeading(doc, "Fotky pracovní");
        photoHelper.addPhotoGrid(doc, subject.getPhotoFilenames());
    }

    // ==================== COMPARABLE SECTION ====================

    public void addComparableSection(XWPFDocument doc, ComparableProperty comp, boolean isSold) {
        // Nadpis
        String heading = String.format("Byt %s, %s m², ul. %s, %s – %s",
                safe(comp.getDisposition()), comp.getArea(),
                safe(comp.getStreet()), safe(comp.getCity()), safe(comp.getDistrict()));

        XWPFParagraph headP = doc.createParagraph();
        headP.setSpacingBefore(200);
        headP.setSpacingAfter(100);
        addStyledRun(headP, heading, 13, true, COLOR_PRIMARY);

        // Cena
        String priceText;
        if (isSold) {
            priceText = "Prodáno za " + comp.getFormattedPrice();
            if (comp.getSoldDate() != null && !comp.getSoldDate().isBlank())
                priceText += " v " + comp.getSoldDate();
            if (comp.getSoldDuration() != null && !comp.getSoldDuration().isBlank())
                priceText += " za " + comp.getSoldDuration();
        } else {
            priceText = comp.getFormattedPrice();
        }

        XWPFParagraph priceP = doc.createParagraph();
        priceP.setSpacingAfter(200);
        addStyledRun(priceP, priceText, 14, true, isSold ? COLOR_GREEN : COLOR_ACCENT);

        // Fotky
        if (!comp.getPhotoFilenames().isEmpty()) {
            photoHelper.addPhotoGrid(doc, comp.getPhotoFilenames());
        }

        // Popis se zvýrazněním
        if (comp.getDescriptionText() != null && !comp.getDescriptionText().isBlank()) {
            addDescriptionWithHighlights(doc, comp.getDescriptionText(), comp.getHighlights());
        }

        // Zpětná vazba
        if (comp.getBrokerFeedback() != null && !comp.getBrokerFeedback().isBlank()) {
            addEmptyLines(doc, 1);
            XWPFParagraph fbP = doc.createParagraph();
            fbP.setSpacingBefore(120);

            XWPFRun labelRun = addStyledRun(fbP, "Zpětná vazba od makléře: ", 10, true, null);
            labelRun.setUnderline(UnderlinePatterns.SINGLE);

            XWPFRun textRun = addStyledRun(fbP, comp.getBrokerFeedback(), 10, false, COLOR_GRAY);
            textRun.setItalic(true);
        }
    }

    // ==================== PRICING PAGE ====================

    public void addPricingPage(XWPFDocument doc, StaProject project) {
        Pricing pricing = project.getPricing();

        addSectionHeading(doc, "Návrh pásma prodejní ceny Vaší nemovitosti");

        // Klady
        List<String> positives = pricing.getPositivesList();
        if (!positives.isEmpty()) {
            addSubHeading(doc, "Kladné stránky nemovitosti:");
            positives.forEach(item -> addBulletItem(doc, item));
        }

        // Zápory
        List<String> negatives = pricing.getNegativesList();
        if (!negatives.isEmpty()) {
            addEmptyLines(doc, 1);
            addSubHeading(doc, "Záporné stránky nemovitosti:");
            negatives.forEach(item -> addBulletItem(doc, item));
        }

        // Doporučení
        if (pricing.getRecommendation() != null && !pricing.getRecommendation().isBlank()) {
            addEmptyLines(doc, 1);
            addSubHeading(doc, "Doporučení:");
            addBulletItem(doc, pricing.getRecommendation());
        }

        addEmptyLines(doc, 1);

        // Úvodní text
        XWPFParagraph introP = doc.createParagraph();
        introP.setSpacingAfter(100);
        addStyledRun(introP, "Vzhledem k výše uvedeným skutečnostem a nabídkám v dané lokalitě "
                + "doporučuji cenové rozmezí pro prodej Vaší nemovitosti", 11, false, null);

        // Cenové rozmezí
        String range = pricing.getFormattedRange();
        if (!range.isBlank()) {
            XWPFParagraph rangeP = doc.createParagraph();
            rangeP.setAlignment(ParagraphAlignment.CENTER);
            rangeP.setSpacingBefore(200);
            rangeP.setSpacingAfter(200);
            addStyledRun(rangeP, range, 16, true, COLOR_PRIMARY);
        }

        // Počáteční cena
        if (pricing.getStartPrice() > 0) {
            XWPFParagraph startP = doc.createParagraph();
            startP.setSpacingBefore(100);
            addStyledRun(startP, "Jako ", 11, false, null);

            XWPFRun boldRun = addStyledRun(startP,
                    "počáteční prodejní cenu navrhuji začít s cenou " + pricing.getFormattedStartPrice(),
                    11, true, null);
            boldRun.setUnderline(UnderlinePatterns.SINGLE);

            addStyledRun(startP, " s případným postupným upravováním.", 11, false, null);
        }
    }

    // ==================== AGENT FOOTER ====================

    public void addAgentFooter(XWPFDocument doc, StaProject project) {
        addEmptyLines(doc, 2);

        XWPFParagraph labelP = doc.createParagraph();
        XWPFRun labelRun = addStyledRun(labelP, "Vypracoval", 10, false, COLOR_GRAY);
        labelRun.setItalic(true);

        addEmptyLines(doc, 1);

        // Kontaktní tabulka
        XWPFTable footerTable = doc.createTable(1, 2);
        footerTable.setWidth(CONTENT_WIDTH + "");
        setTableBordersNone(footerTable);

        XWPFTableRow row = footerTable.getRow(0);

        // Levý sloupec — kontakt
        XWPFTableCell leftCell = row.getCell(0);
        setCellWidth(leftCell, CONTENT_WIDTH / 2);

        addContactLine(leftCell.getParagraphArray(0), safe(project.getAgentName()), true, 12, COLOR_BLACK);
        addContactLine(leftCell.addParagraph(), safe(project.getAgentTitle()), false, 10, COLOR_GRAY);
        addContactLine(leftCell.addParagraph(), "M: " + safe(project.getAgentPhone()), false, 9, COLOR_GRAY);
        addContactLine(leftCell.addParagraph(), "E: " + safe(project.getAgentEmail()), false, 9, COLOR_GRAY);
        addContactLine(leftCell.addParagraph(), safe(project.getAgencyName()), true, 10, COLOR_BLACK);
        addContactLine(leftCell.addParagraph(), safe(project.getAgencyAddress()), false, 8, COLOR_GRAY);
        addContactLine(leftCell.addParagraph(), safe(project.getAgentWebsite()), false, 9, COLOR_PRIMARY);

        // Pravý sloupec — logo placeholder
        XWPFTableCell rightCell = row.getCell(1);
        setCellWidth(rightCell, CONTENT_WIDTH / 2);
        XWPFParagraph logoP = rightCell.getParagraphArray(0);
        if (logoP == null) logoP = rightCell.addParagraph();
        logoP.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun logoRun = addStyledRun(logoP, "[Logo placeholder]", 9, false, COLOR_GRAY);
        logoRun.setItalic(true);
    }

    // ==================== PRIVATE HELPERS ====================

    private void addDescriptionWithHighlights(XWPFDocument doc, String text, List<String> highlights) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(100);
        p.setSpacingAfter(100);

        if (highlights == null || highlights.isEmpty()) {
            addStyledRun(p, text, 9, false, COLOR_GRAY);
            return;
        }

        String remaining = text;
        for (String highlight : highlights) {
            if (highlight == null || highlight.isBlank()) continue;
            int idx = remaining.toLowerCase().indexOf(highlight.toLowerCase());
            if (idx >= 0) {
                if (idx > 0) {
                    addStyledRun(p, remaining.substring(0, idx), 9, false, COLOR_GRAY);
                }
                XWPFRun hlRun = addStyledRun(p, remaining.substring(idx, idx + highlight.length()), 9, true, COLOR_BLACK);
                setRunHighlight(hlRun, COLOR_HIGHLIGHT);
                remaining = remaining.substring(idx + highlight.length());
            }
        }
        if (!remaining.isEmpty()) {
            addStyledRun(p, remaining, 9, false, COLOR_GRAY);
        }
    }

    private String buildSubjectLine(Subject s) {
        StringBuilder sb = new StringBuilder();
        sb.append("byt ").append(safe(s.getDisposition()));
        if (s.getOwnership() != null && !s.getOwnership().isBlank()) sb.append(", ").append(s.getOwnership());
        if (s.getAreaSqm() > 0) sb.append(", ").append(s.getAreaSqm()).append(" m²");
        if (s.getExtras() != null && !s.getExtras().isBlank()) sb.append(" + ").append(s.getExtras());
        return sb.toString();
    }

    private void addSectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(200);
        p.setBorderBottom(Borders.SINGLE);
        addStyledRun(p, text, 16, true, COLOR_PRIMARY);
    }

    private void addSubHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        p.setSpacingAfter(80);
        XWPFRun run = addStyledRun(p, text, 11, true, null);
        run.setUnderline(UnderlinePatterns.SINGLE);
    }

    private void addLabelValue(XWPFDocument doc, String label, String value, int fontSize) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(80);
        addStyledRun(p, label, fontSize, true, null);
        addStyledRun(p, value, fontSize, false, null);
    }

    private void addBulletItem(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(720);
        p.setSpacingAfter(40);
        addStyledRun(p, "•  " + text, 10, false, null);
    }

    private void addContactLine(XWPFParagraph p, String text, boolean bold, int size, String color) {
        if (p == null) return;
        addStyledRun(p, text, size, bold, color);
    }
}