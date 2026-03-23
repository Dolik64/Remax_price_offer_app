package cz.stabuilder.service.docx;

import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.StaProject;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.math.BigInteger;
import java.util.List;

import static cz.stabuilder.service.docx.DocxStyleHelper.*;

/**
 * Generuje srovnávací tabulku nemovitostí v DOCX.
 */
public class DocxTableBuilder {

    private static final int[] COL_WIDTHS = {2800, 2000, 1800, 3038};

    private DocxTableBuilder() {}

    /**
     * Přidá kompletní srovnávací tabulku do dokumentu.
     */
    public static void addComparisonTable(XWPFDocument doc, StaProject project) {
        addSectionHeading(doc, "Nabízené / prodané byty – srovnání");

        // Podnadpis
        XWPFParagraph subP = doc.createParagraph();
        subP.setSpacingAfter(200);
        XWPFRun subRun = subP.createRun();
        subRun.setText("Porovnatelné panelové byty v lokalitě " + safe(project.getSubject().getAddress()));
        subRun.setFontSize(10);
        subRun.setFontFamily(FONT);
        subRun.setColor(COLOR_GRAY);
        subRun.setItalic(true);

        List<ComparableProperty> offers = project.getOffers();
        List<ComparableProperty> sold = project.getSold();

        int totalRows = 1;
        if (!offers.isEmpty()) totalRows += 1 + offers.size();
        if (!sold.isEmpty()) totalRows += 1 + sold.size();

        XWPFTable table = doc.createTable(totalRows, 4);
        table.setWidth(CONTENT_WIDTH + "");

        addHeaderRow(table);

        int row = 1;
        if (!offers.isEmpty()) {
            addCategoryRow(table, row, "Nabídka", COLOR_OFFER_BG);
            row++;
            for (ComparableProperty c : offers) {
                addDataRow(table, row, c, false);
                row++;
            }
        }
        if (!sold.isEmpty()) {
            addCategoryRow(table, row, "Prodáno", COLOR_SOLD_BG);
            row++;
            for (ComparableProperty c : sold) {
                addDataRow(table, row, c, true);
                row++;
            }
        }

        setTableBordersThin(table);
    }

    // === Header ===

    private static void addHeaderRow(XWPFTable table) {
        XWPFTableRow headerRow = table.getRow(0);
        String[] headers = {"Lokalita", "Prodejní cena", "Cena za m²", "Výhody / Nevýhody"};
        for (int i = 0; i < 4; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setCellWidth(cell, COL_WIDTHS[i]);
            setCellColor(cell, COLOR_TABLE_HEADER);
            setCellMargins(cell, 60, 80, 60, 80);

            XWPFParagraph p = cell.getParagraphArray(0);
            if (p == null) p = cell.addParagraph();
            addStyledRun(p, headers[i], 9, true, COLOR_WHITE);
        }
    }

    // === Category row (Nabídka / Prodáno) ===

    private static void addCategoryRow(XWPFTable table, int rowIdx, String label, String bgColor) {
        XWPFTableRow row = table.getRow(rowIdx);
        for (int i = 0; i < 4; i++) {
            XWPFTableCell cell = row.getCell(i);
            setCellWidth(cell, COL_WIDTHS[i]);
            setCellColor(cell, bgColor);
            setCellMargins(cell, 40, 60, 40, 60);

            CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            if (i == 0) {
                tcPr.addNewGridSpan().setVal(BigInteger.valueOf(4));
                XWPFParagraph p = cell.getParagraphArray(0);
                if (p == null) p = cell.addParagraph();
                addStyledRun(p, label, 9, true, COLOR_PRIMARY);
            } else {
                tcPr.addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    // === Data row ===

    private static void addDataRow(XWPFTable table, int rowIdx, ComparableProperty c, boolean isSold) {
        XWPFTableRow row = table.getRow(rowIdx);

        for (int i = 0; i < 4; i++) {
            XWPFTableCell cell = row.getCell(i);
            setCellWidth(cell, COL_WIDTHS[i]);
            setCellMargins(cell, 40, 60, 40, 60);

            XWPFParagraph p = cell.getParagraphArray(0);
            if (p == null) p = cell.addParagraph();

            switch (i) {
                case 0 -> addLocalityCell(p, c);
                case 1 -> addStyledRun(p, c.getFormattedPrice(), 9, true, null);
                case 2 -> addStyledRun(p, c.getFormattedPricePerSqm(), 9, true, null);
                case 3 -> addDetailsCell(p, c, isSold);
            }
        }
    }

    private static void addLocalityCell(XWPFParagraph p, ComparableProperty c) {
        XWPFRun streetRun = addStyledRun(p, safe(c.getStreet()) + ",", 9, true, null);
        streetRun.addBreak();
        addStyledRun(p, safe(c.getDisposition()) + ", " + c.getArea() + " m²", 8, false, COLOR_GRAY);
    }

    private static void addDetailsCell(XWPFParagraph p, ComparableProperty c, boolean isSold) {
        StringBuilder details = new StringBuilder();
        if (c.getFloor() != null && !c.getFloor().isBlank()) details.append(c.getFloor());
        if (c.getExtras() != null && !c.getExtras().isBlank()) {
            if (!details.isEmpty()) details.append(", ");
            details.append(c.getExtras());
        }
        if (c.getCondition() != null && !c.getCondition().isBlank()) {
            if (!details.isEmpty()) details.append(", ");
            details.append(c.getCondition());
        }

        addStyledRun(p, details.toString(), 8, false, null);

        if (isSold && c.getSoldDate() != null && !c.getSoldDate().isBlank()) {
            p.createRun().addBreak();
            String soldText = "Prodáno v " + c.getSoldDate();
            if (c.getSoldDuration() != null && !c.getSoldDuration().isBlank()) {
                soldText += " za " + c.getSoldDuration();
            }
            addStyledRun(p, soldText, 8, true, COLOR_GREEN);
        }
    }

    // === Section heading ===

    private static void addSectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(200);
        p.setBorderBottom(Borders.SINGLE);
        addStyledRun(p, text, 16, true, COLOR_PRIMARY);
    }
}