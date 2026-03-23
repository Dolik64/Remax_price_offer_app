package cz.stabuilder.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.Pricing;
import cz.stabuilder.model.StaProject;
import cz.stabuilder.model.Subject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(0, 56, 101);
    private static final DeviceRgb COLOR_ACCENT = new DeviceRgb(200, 16, 46);
    private static final DeviceRgb COLOR_TABLE_HEADER = new DeviceRgb(0, 56, 101);
    private static final DeviceRgb COLOR_TABLE_OFFER = new DeviceRgb(220, 235, 250);
    private static final DeviceRgb COLOR_TABLE_SOLD = new DeviceRgb(220, 245, 220);
    private static final DeviceRgb COLOR_HIGHLIGHT = new DeviceRgb(255, 255, 150);
    private static final DeviceRgb COLOR_GRAY = new DeviceRgb(100, 100, 100);

    private final PhotoService photoService;

    public PdfExportService(PhotoService photoService) {
        this.photoService = photoService;
    }

    public byte[] generatePdf(StaProject project) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        PdfFont fontRegular = PdfFontFactory.createFont("Helvetica", PdfEncodings.CP1250);
        PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold", PdfEncodings.CP1250);
        doc.setFont(fontRegular);
        doc.setFontSize(10);

        addTitlePage(doc, project, fontRegular, fontBold);

        if (!project.getSubject().getPhotoFilenames().isEmpty()) {
            doc.add(new AreaBreak());
            addSubjectPhotosPage(doc, project.getSubject(), fontBold);
        }

        List<ComparableProperty> offers = project.getOffers();
        List<ComparableProperty> sold = project.getSold();

        if (!offers.isEmpty()) {
            doc.add(new AreaBreak());
            addSectionTitle(doc, "Podobné nabídky aktuálně v prodeji", fontBold);
            for (ComparableProperty comp : offers) {
                addComparablePage(doc, comp, fontRegular, fontBold, false);
            }
        }

        if (!sold.isEmpty()) {
            doc.add(new AreaBreak());
            addSectionTitle(doc, "Nedávno prodané nemovitosti", fontBold);
            for (ComparableProperty comp : sold) {
                addComparablePage(doc, comp, fontRegular, fontBold, true);
            }
        }

        if (!project.getComparables().isEmpty()) {
            doc.add(new AreaBreak());
            addComparisonTable(doc, project, fontRegular, fontBold);
        }

        doc.add(new AreaBreak());
        addPricingPage(doc, project, fontRegular, fontBold);
        addAgentFooter(doc, project, fontRegular, fontBold);

        doc.close();
        return baos.toByteArray();
    }

    // === TITLE PAGE ===

    private void addTitlePage(Document doc, StaProject project, PdfFont fontRegular, PdfFont fontBold) {
        Subject subject = project.getSubject();
        doc.add(new Paragraph("\n\n\n"));
        doc.add(new Paragraph("SROVNÁVACÍ TRŽNÍ ANALÝZA")
                .setFont(fontBold).setFontSize(26).setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("\n"));

        String dateStr = subject.getDate() != null
                ? subject.getDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "";
        doc.add(new Paragraph(dateStr).setFont(fontBold).setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("\n"));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        addInfoRow(infoTable, "Vypracováno pro:", safe(subject.getClientName()), fontBold, fontRegular);
        addInfoRow(infoTable, "Předmět prodeje:", buildSubjectLine(subject), fontBold, fontRegular);
        addInfoRow(infoTable, "Adresa:", safe(subject.getAddress()), fontBold, fontRegular);
        doc.add(infoTable);
        doc.add(new Paragraph("\n"));

        if (subject.getDescription() != null && !subject.getDescription().isBlank()) {
            doc.add(new Paragraph("Popis nemovitosti:")
                    .setFont(fontBold).setFontSize(12).setFontColor(COLOR_PRIMARY));
            doc.add(new Paragraph(subject.getDescription()).setFontSize(10).setFontColor(COLOR_GRAY));
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

    private void addInfoRow(Table table, String label, String value, PdfFont fontBold, PdfFont fontRegular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(fontBold).setFontSize(11))
                .setBorder(Border.NO_BORDER).setPaddingBottom(4));
        table.addCell(new Cell().add(new Paragraph(value).setFont(fontRegular).setFontSize(11))
                .setBorder(Border.NO_BORDER).setPaddingBottom(4));
    }

    // === SUBJECT PHOTOS ===

    private void addSubjectPhotosPage(Document doc, Subject subject, PdfFont fontBold) {
        doc.add(new Paragraph("Fotky nemovitosti")
                .setFont(fontBold).setFontSize(14).setFontColor(COLOR_PRIMARY));
        addPhotoGrid(doc, subject.getPhotoFilenames());
    }

    // === COMPARABLE ===

    private void addComparablePage(Document doc, ComparableProperty comp,
                                   PdfFont fontRegular, PdfFont fontBold, boolean isSold) {
        doc.add(new Paragraph("\n"));
        String title = String.format("Byt %s, %s m², %s, %s – %s",
                safe(comp.getDisposition()), comp.getArea(),
                safe(comp.getStreet()), safe(comp.getCity()), safe(comp.getDistrict()));
        doc.add(new Paragraph(title).setFont(fontBold).setFontSize(12).setFontColor(COLOR_PRIMARY));

        String priceLabel;
        if (isSold) {
            priceLabel = String.format("Prodáno za %,d Kč", comp.getPriceKc()).replace(",", ".");
            if (comp.getSoldDate() != null && !comp.getSoldDate().isBlank())
                priceLabel += " v " + comp.getSoldDate();
            if (comp.getSoldDuration() != null && !comp.getSoldDuration().isBlank())
                priceLabel += " za " + comp.getSoldDuration();
        } else {
            priceLabel = String.format("%,d Kč", comp.getPriceKc()).replace(",", ".");
        }
        doc.add(new Paragraph(priceLabel).setFont(fontBold).setFontSize(14)
                .setFontColor(isSold ? new DeviceRgb(46, 125, 50) : COLOR_ACCENT));

        if (!comp.getPhotoFilenames().isEmpty()) addPhotoGrid(doc, comp.getPhotoFilenames());

        if (comp.getDescriptionText() != null && !comp.getDescriptionText().isBlank()) {
            Paragraph descP = new Paragraph().setFontSize(9).setFontColor(COLOR_GRAY);
            if (comp.getHighlights().isEmpty()) {
                descP.add(new Text(comp.getDescriptionText()).setFont(fontRegular));
            } else {
                String remaining = comp.getDescriptionText();
                for (String hl : comp.getHighlights()) {
                    int idx = remaining.toLowerCase().indexOf(hl.toLowerCase());
                    if (idx >= 0) {
                        if (idx > 0) descP.add(new Text(remaining.substring(0, idx)).setFont(fontRegular));
                        descP.add(new Text(remaining.substring(idx, idx + hl.length()))
                                .setFont(fontBold).setBackgroundColor(COLOR_HIGHLIGHT));
                        remaining = remaining.substring(idx + hl.length());
                    }
                }
                if (!remaining.isEmpty()) descP.add(new Text(remaining).setFont(fontRegular));
            }
            doc.add(descP);
        }

        if (comp.getBrokerFeedback() != null && !comp.getBrokerFeedback().isBlank()) {
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("Zpětná vazba od makléře:")
                    .setFont(fontBold).setFontSize(10).setUnderline().setFontColor(COLOR_PRIMARY));
            doc.add(new Paragraph(comp.getBrokerFeedback())
                    .setFontSize(9).setFontColor(COLOR_GRAY).setItalic());
        }
    }

    // === PHOTO GRID ===

    private void addPhotoGrid(Document doc, List<String> filenames) {
        if (filenames.isEmpty()) return;
        Table t = new Table(UnitValue.createPercentArray(new float[]{33, 33, 33}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8).setMarginBottom(8);
        for (String fn : filenames) {
            Cell cell = new Cell().setBorder(Border.NO_BORDER).setPadding(3);
            try {
                byte[] bytes = photoService.getPhotoBytes(fn);
                Image img = new Image(ImageDataFactory.create(bytes));
                img.setWidth(UnitValue.createPercentValue(100)).setAutoScale(true);
                cell.add(img);
            } catch (IOException e) {
                cell.add(new Paragraph("[" + fn + "]").setFontSize(8).setFontColor(ColorConstants.GRAY));
            }
            t.addCell(cell);
        }
        int rem = (3 - filenames.size() % 3) % 3;
        for (int i = 0; i < rem; i++) t.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(t);
    }

    // === COMPARISON TABLE ===

    private void addComparisonTable(Document doc, StaProject project, PdfFont fontRegular, PdfFont fontBold) {
        addSectionTitle(doc, "Nabízené / prodané byty – srovnání", fontBold);

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 18, 17, 35}))
                .setWidth(UnitValue.createPercentValue(100));
        for (String h : new String[]{"Lokalita", "Prodejní cena", "Cena za m²", "Výhody / Nevýhody"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_TABLE_HEADER)
                    .setBorder(new SolidBorder(ColorConstants.WHITE, 0.5f)).setPadding(6));
        }

        List<ComparableProperty> offers = project.getOffers();
        List<ComparableProperty> sold = project.getSold();

        if (!offers.isEmpty()) {
            table.addCell(new Cell(1, 4).add(new Paragraph("Nabídka").setFont(fontBold).setFontSize(9))
                    .setBackgroundColor(COLOR_TABLE_OFFER)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
            offers.forEach(c -> addTableRow(table, c, fontRegular, fontBold, false));
        }
        if (!sold.isEmpty()) {
            table.addCell(new Cell(1, 4).add(new Paragraph("Prodáno").setFont(fontBold).setFontSize(9))
                    .setBackgroundColor(COLOR_TABLE_SOLD)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
            sold.forEach(c -> addTableRow(table, c, fontRegular, fontBold, true));
        }
        doc.add(table);
    }

    private void addTableRow(Table table, ComparableProperty c, PdfFont fontRegular, PdfFont fontBold, boolean isSold) {
        SolidBorder b = new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f);

        Paragraph loc = new Paragraph()
                .add(new Text(safe(c.getStreet()) + ",\n").setFont(fontBold).setFontSize(9))
                .add(new Text(safe(c.getDisposition()) + ", " + c.getArea() + " m²").setFont(fontRegular).setFontSize(8));
        table.addCell(new Cell().add(loc).setBorder(b).setPadding(5));

        table.addCell(new Cell().add(new Paragraph(String.format("%,d Kč", c.getPriceKc()).replace(",", "."))
                .setFont(fontBold).setFontSize(9)).setBorder(b).setPadding(5));

        table.addCell(new Cell().add(new Paragraph(String.format("%,d Kč", c.getPricePerSqm()).replace(",", "."))
                .setFont(fontBold).setFontSize(9)).setBorder(b).setPadding(5));

        StringBuilder det = new StringBuilder();
        if (c.getFloor() != null && !c.getFloor().isBlank()) det.append(c.getFloor()).append(", ");
        if (c.getExtras() != null && !c.getExtras().isBlank()) det.append(c.getExtras()).append(",\n");
        if (c.getCondition() != null && !c.getCondition().isBlank()) det.append(c.getCondition());
        if (isSold && c.getSoldDate() != null && !c.getSoldDate().isBlank()) {
            det.append("\nProdáno v ").append(c.getSoldDate());
            if (c.getSoldDuration() != null && !c.getSoldDuration().isBlank())
                det.append(" za ").append(c.getSoldDuration());
        }
        table.addCell(new Cell().add(new Paragraph(det.toString()).setFont(fontRegular).setFontSize(8))
                .setBorder(b).setPadding(5));
    }

    // === PRICING ===

    private void addPricingPage(Document doc, StaProject project, PdfFont fontRegular, PdfFont fontBold) {
        Pricing pricing = project.getPricing();
        addSectionTitle(doc, "Návrh pásma prodejní ceny Vaší nemovitosti", fontBold);

        List<String> positives = pricing.getPositivesList();
        if (!positives.isEmpty()) {
            doc.add(new Paragraph("Kladné stránky nemovitosti:")
                    .setFont(fontBold).setFontSize(11).setUnderline().setMarginTop(12));
            for (String pos : positives) {
                doc.add(new Paragraph("• " + pos).setFontSize(10).setMarginLeft(20));
            }
        }

        List<String> negatives = pricing.getNegativesList();
        if (!negatives.isEmpty()) {
            doc.add(new Paragraph("Záporné stránky nemovitosti:")
                    .setFont(fontBold).setFontSize(11).setUnderline().setMarginTop(12));
            for (String neg : negatives) {
                doc.add(new Paragraph("• " + neg).setFontSize(10).setMarginLeft(20));
            }
        }

        if (pricing.getRecommendation() != null && !pricing.getRecommendation().isBlank()) {
            doc.add(new Paragraph("Doporučení:").setFont(fontBold).setFontSize(11).setMarginTop(12));
            doc.add(new Paragraph("• " + pricing.getRecommendation()).setFontSize(10).setMarginLeft(20));
        }

        doc.add(new Paragraph("\n"));
        String range = pricing.getPriceRangeFormatted();
        if (!range.isBlank()) {
            doc.add(new Paragraph("od " + range).setFont(fontBold).setFontSize(16)
                    .setFontColor(COLOR_PRIMARY).setTextAlignment(TextAlignment.CENTER).setMarginTop(12));
        }

        String startPrice = pricing.getStartPriceFormatted();
        if (!startPrice.isBlank()) {
            doc.add(new Paragraph("Jako počáteční prodejní cenu navrhuji začít s cenou " + startPrice)
                    .setFont(fontBold).setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8).setUnderline());
        }
    }

    // === AGENT FOOTER ===

    private void addAgentFooter(Document doc, StaProject project, PdfFont fontRegular, PdfFont fontBold) {
        doc.add(new Paragraph("\n\n"));
        doc.add(new Paragraph("Vypracoval").setFontSize(10).setItalic().setFontColor(COLOR_GRAY));

        Table footer = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);
        Paragraph contact = new Paragraph()
                .add(new Text(safe(project.getAgentName()) + "\n").setFont(fontBold).setFontSize(12))
                .add(new Text(safe(project.getAgentTitle()) + "\n").setFont(fontRegular).setFontSize(10))
                .add(new Text("M: " + safe(project.getAgentPhone()) + "\n").setFontSize(9))
                .add(new Text("E: " + safe(project.getAgentEmail()) + "\n").setFontSize(9))
                .add(new Text(safe(project.getAgencyName()) + "\n").setFont(fontBold).setFontSize(10))
                .add(new Text(safe(project.getAgencyAddress()) + "\n").setFontSize(8))
                .add(new Text(safe(project.getAgentWebsite())).setFontSize(9).setFontColor(COLOR_PRIMARY));
        footer.addCell(new Cell().add(contact).setBorder(Border.NO_BORDER));
        footer.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(footer);
    }

    // === HELPERS ===

    private void addSectionTitle(Document doc, String title, PdfFont fontBold) {
        doc.add(new Paragraph(title).setFont(fontBold).setFontSize(16).setFontColor(COLOR_PRIMARY)
                .setMarginBottom(12).setBorderBottom(new SolidBorder(COLOR_PRIMARY, 2)));
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}