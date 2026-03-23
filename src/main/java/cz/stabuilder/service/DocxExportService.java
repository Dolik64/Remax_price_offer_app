package cz.stabuilder.service;

import cz.stabuilder.model.ComparableProperty;
import cz.stabuilder.model.StaProject;
import cz.stabuilder.service.docx.DocxContentBuilder;
import cz.stabuilder.service.docx.DocxPhotoHelper;
import cz.stabuilder.service.docx.DocxStyleHelper;
import cz.stabuilder.service.docx.DocxTableBuilder;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Hlavní služba pro generování DOCX Srovnávací tržní analýzy.
 * Deleguje na specializované buildery.
 */
@Service
public class DocxExportService {

    private final DocxContentBuilder contentBuilder;

    public DocxExportService(PhotoService photoService) {
        DocxPhotoHelper photoHelper = new DocxPhotoHelper(photoService);
        this.contentBuilder = new DocxContentBuilder(photoHelper);
    }

    /**
     * Vygeneruje kompletní DOCX a vrátí jako byte[].
     */
    public byte[] generateDocx(StaProject project) throws IOException {
        XWPFDocument doc = new XWPFDocument();

        // Nastavení stránky A4
        DocxStyleHelper.setupPageLayout(doc);

        // Titulní strana
        contentBuilder.addTitlePage(doc, project);

        // Fotky předmětu prodeje
        if (!project.getSubject().getPhotoFilenames().isEmpty()) {
            DocxStyleHelper.addPageBreak(doc);
            contentBuilder.addSubjectPhotosPage(doc, project.getSubject());
        }

        // Srovnatelné nemovitosti — v nabídce
        List<ComparableProperty> offers = project.getOffers();
        if (!offers.isEmpty()) {
            DocxStyleHelper.addPageBreak(doc);
            addSectionHeading(doc, "Podobné nabídky aktuálně v prodeji");
            for (int i = 0; i < offers.size(); i++) {
                if (i > 0) DocxStyleHelper.addPageBreak(doc);
                contentBuilder.addComparableSection(doc, offers.get(i), false);
            }
        }

        // Srovnatelné nemovitosti — prodané
        List<ComparableProperty> sold = project.getSold();
        if (!sold.isEmpty()) {
            DocxStyleHelper.addPageBreak(doc);
            addSectionHeading(doc, "Nedávno prodané nemovitosti");
            for (int i = 0; i < sold.size(); i++) {
                if (i > 0) DocxStyleHelper.addPageBreak(doc);
                contentBuilder.addComparableSection(doc, sold.get(i), true);
            }
        }

        // Srovnávací tabulka
        if (!project.getComparables().isEmpty()) {
            DocxStyleHelper.addPageBreak(doc);
            DocxTableBuilder.addComparisonTable(doc, project);
        }

        // Cenové doporučení
        DocxStyleHelper.addPageBreak(doc);
        contentBuilder.addPricingPage(doc, project);

        // Patička s kontaktem
        contentBuilder.addAgentFooter(doc, project);

        // Export
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    private void addSectionHeading(XWPFDocument doc, String text) {
        var p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(200);
        p.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        DocxStyleHelper.addStyledRun(p, text, 16, true, DocxStyleHelper.COLOR_PRIMARY);
    }
}