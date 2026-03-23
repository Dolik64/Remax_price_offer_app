package cz.stabuilder.service.docx;

import cz.stabuilder.service.PhotoService;
import org.apache.poi.xwpf.usermodel.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import static cz.stabuilder.service.docx.DocxStyleHelper.*;

/**
 * Vkládání fotek do DOCX — grid 3×2 s automatickým škálováním.
 */
public class DocxPhotoHelper {

    private final PhotoService photoService;

    public DocxPhotoHelper(PhotoService photoService) {
        this.photoService = photoService;
    }

    /**
     * Přidá grid fotek (3 sloupce) do dokumentu.
     */
    public void addPhotoGrid(XWPFDocument doc, List<String> filenames) {
        if (filenames.isEmpty()) return;

        int cols = 3;
        int colWidthDxa = CONTENT_WIDTH / cols;

        for (int rowStart = 0; rowStart < filenames.size(); rowStart += cols) {
            int rowEnd = Math.min(rowStart + cols, filenames.size());
            int cellCount = rowEnd - rowStart;

            XWPFTable photoTable = doc.createTable(1, cols);
            photoTable.setWidth(CONTENT_WIDTH + "");
            setTableBordersNone(photoTable);

            XWPFTableRow row = photoTable.getRow(0);

            for (int i = 0; i < cols; i++) {
                XWPFTableCell cell = row.getCell(i);
                setCellWidth(cell, colWidthDxa);
                setCellMargins(cell, 40, 40, 40, 40);

                if (i < cellCount) {
                    addPhotoToCell(cell, filenames.get(rowStart + i), colWidthDxa - 120);
                } else {
                    cell.setText("");
                }
            }

            // Spacing po tabulce
            XWPFParagraph spacer = doc.createParagraph();
            spacer.setSpacingAfter(40);
        }
    }

    /**
     * Vloží fotku do buňky tabulky se správným škálováním.
     */
    private void addPhotoToCell(XWPFTableCell cell, String filename, int maxWidthDxa) {
        try {
            byte[] imageBytes = photoService.getPhotoBytes(filename);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                cell.setText("[" + filename + "]");
                return;
            }

            int imgW = img.getWidth();
            int imgH = img.getHeight();

            // 1 DXA = 635 EMU
            long maxWidthEmu = (long) maxWidthDxa * 635;
            long targetWidthEmu = maxWidthEmu;
            long targetHeightEmu = (long) ((double) imgH / imgW * targetWidthEmu);

            // Max výška 5cm (1cm = 360000 EMU)
            long maxHeightEmu = 5 * 360000L;
            if (targetHeightEmu > maxHeightEmu) {
                targetHeightEmu = maxHeightEmu;
                targetWidthEmu = (long) ((double) imgW / imgH * targetHeightEmu);
            }

            XWPFParagraph p = cell.getParagraphArray(0);
            if (p == null) p = cell.addParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = p.createRun();

            String lower = filename.toLowerCase();
            int picType = lower.endsWith(".png") ? XWPFDocument.PICTURE_TYPE_PNG : XWPFDocument.PICTURE_TYPE_JPEG;

            run.addPicture(new ByteArrayInputStream(imageBytes), picType, filename,
                    targetWidthEmu, targetHeightEmu);

        } catch (Exception e) {
            cell.setText("[Fotka: " + filename + "]");
        }
    }
}