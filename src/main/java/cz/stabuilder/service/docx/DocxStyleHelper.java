package cz.stabuilder.service.docx;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;

/**
 * Pomocné metody pro formátování DOCX — barvy, okraje, buňky, fonty.
 */
public class DocxStyleHelper {

    // Barvy (hex bez #)
    public static final String COLOR_PRIMARY = "003865";
    public static final String COLOR_ACCENT = "C8102E";
    public static final String COLOR_LIGHT_BLUE = "D5E8F0";
    public static final String COLOR_TABLE_HEADER = "003865";
    public static final String COLOR_OFFER_BG = "DCE7F5";
    public static final String COLOR_SOLD_BG = "D5F0D5";
    public static final String COLOR_HIGHLIGHT = "FFFF99";
    public static final String COLOR_GRAY = "666666";
    public static final String COLOR_WHITE = "FFFFFF";
    public static final String COLOR_BLACK = "000000";
    public static final String COLOR_GREEN = "2E7D32";

    // Rozměry A4 v DXA
    public static final int PAGE_WIDTH = 11906;
    public static final int PAGE_HEIGHT = 16838;
    public static final int MARGIN = 1134;
    public static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    public static final String FONT = "Arial";

    private DocxStyleHelper() {}

    // === Stránka ===

    public static void setupPageLayout(XWPFDocument doc) {
        CTDocument1 ctDoc = doc.getDocument();
        CTBody ctBody = ctDoc.getBody();
        if (ctBody == null) ctBody = ctDoc.addNewBody();
        CTSectPr sectPr = ctBody.isSetSectPr() ? ctBody.getSectPr() : ctBody.addNewSectPr();

        CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pgSz.setW(BigInteger.valueOf(PAGE_WIDTH));
        pgSz.setH(BigInteger.valueOf(PAGE_HEIGHT));

        CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pgMar.setTop(BigInteger.valueOf(MARGIN));
        pgMar.setBottom(BigInteger.valueOf(MARGIN));
        pgMar.setLeft(BigInteger.valueOf(MARGIN));
        pgMar.setRight(BigInteger.valueOf(MARGIN));
    }

    // === Buňky tabulky ===

    public static void setCellWidth(XWPFTableCell cell, int widthDxa) {
        CTTcPr tcPr = getOrCreateTcPr(cell);
        CTTblWidth w = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        w.setType(STTblWidth.DXA);
        w.setW(BigInteger.valueOf(widthDxa));
    }

    public static void setCellColor(XWPFTableCell cell, String hexColor) {
        CTTcPr tcPr = getOrCreateTcPr(cell);
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(hexColor);
    }

    public static void setCellMargins(XWPFTableCell cell, int top, int left, int bottom, int right) {
        CTTcPr tcPr = getOrCreateTcPr(cell);
        CTTcMar mar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();

        setMarginValue(mar.isSetTop() ? mar.getTop() : mar.addNewTop(), top);
        setMarginValue(mar.isSetLeft() ? mar.getLeft() : mar.addNewLeft(), left);
        setMarginValue(mar.isSetBottom() ? mar.getBottom() : mar.addNewBottom(), bottom);
        setMarginValue(mar.isSetRight() ? mar.getRight() : mar.addNewRight(), right);
    }

    // === Okraje tabulky ===

    public static void setTableBordersNone(XWPFTable table) {
        CTTblBorders borders = getOrCreateTableBorders(table);
        setBorderNone(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorderNone(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorderNone(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorderNone(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setBorderNone(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH());
        setBorderNone(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV());
    }

    public static void setTableBordersThin(XWPFTable table) {
        CTTblBorders borders = getOrCreateTableBorders(table);
        setBorderThin(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorderThin(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorderThin(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorderThin(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setBorderThin(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH());
        setBorderThin(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV());
    }

    // === Zvýraznění textu (žluté pozadí) ===

    public static void setRunHighlight(XWPFRun run, String bgColor) {
        CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTShd shd = rpr.isSetShd() ? rpr.getShd() : rpr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(bgColor);
    }

    // === Textové helpery ===

    public static XWPFRun addStyledRun(XWPFParagraph p, String text, int fontSize, boolean bold, String color) {
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setFontFamily(FONT);
        run.setBold(bold);
        if (color != null) run.setColor(color);
        return run;
    }

    public static void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().addBreak(BreakType.PAGE);
    }

    public static void addEmptyLines(XWPFDocument doc, int count) {
        for (int i = 0; i < count; i++) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingAfter(80);
        }
    }

    public static String safe(String s) {
        return s != null ? s : "";
    }

    // === Private ===

    private static CTTcPr getOrCreateTcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private static CTTblBorders getOrCreateTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
        return tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    }

    private static void setMarginValue(CTTblWidth tw, int value) {
        tw.setType(STTblWidth.DXA);
        tw.setW(BigInteger.valueOf(value));
    }

    private static void setBorderNone(CTBorder border) {
        border.setVal(STBorder.NONE);
        border.setSz(BigInteger.ZERO);
        border.setSpace(BigInteger.ZERO);
        border.setColor("FFFFFF");
    }

    private static void setBorderThin(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor("CCCCCC");
    }
}