package com.myapp.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

public class HeaderFooterEvent extends PdfPageEventHelper {
    private final String reportTitle;
    private final Color brandColor = new Color(180, 50, 30);
    private final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, brandColor);
    private final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

    public HeaderFooterEvent(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        
        // --- HEADER ---
        Phrase headerLeft = new Phrase("Halimaw Burger", headerFont);
        Phrase headerRight = new Phrase(reportTitle.toUpperCase(), titleFont);
        
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, headerLeft, document.left(), document.top() + 10, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, headerRight, document.right(), document.top() + 10, 0);
        
        // Divider line
        cb.setLineWidth(1f);
        cb.setColorStroke(brandColor);
        cb.moveTo(document.left(), document.top() + 5);
        cb.lineTo(document.right(), document.top() + 5);
        cb.stroke();

        // --- FOOTER ---
        cb.setLineWidth(0.5f);
        cb.setColorStroke(Color.LIGHT_GRAY);
        cb.moveTo(document.left(), document.bottom() - 5);
        cb.lineTo(document.right(), document.bottom() - 5);
        cb.stroke();

        Phrase footerLeft = new Phrase("Halimaw Burger", footerFont);
        Phrase footerRight = new Phrase(String.format("Page %d", writer.getPageNumber()), footerFont);
        
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, footerLeft, document.left(), document.bottom() - 15, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footerRight, document.right(), document.bottom() - 15, 0);
    }
}
