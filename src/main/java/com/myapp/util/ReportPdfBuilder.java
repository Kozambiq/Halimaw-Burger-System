package com.myapp.util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportPdfBuilder {
    private final Color brandColor = new Color(180, 50, 30);
    private final Color altRowColor = new Color(245, 245, 245);
    private final Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, brandColor);
    private final Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private final Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private final Font fontMetric = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, brandColor);
    private final Font fontTableHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

    public void generateReport(
            String reportTitle,
            String dateFrom,
            String dateTo,
            String generatedBy,
            List<String> columns,
            List<Map<String, String>> rows,
            List<Map<String, String>> summaryMetrics,
            String outputPath) throws Exception {

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        
        // Attach Header/Footer event
        writer.setPageEvent(new HeaderFooterEvent(reportTitle));
        
        document.open();
        document.add(new Paragraph("\n")); // Spacer for header

        // 1. META SECTION (First Page Only)
        addMetaSection(document, reportTitle, dateFrom, dateTo, generatedBy);
        document.add(new Paragraph("\n"));

        // 2. SUMMARY BOXES
        if (summaryMetrics != null && !summaryMetrics.isEmpty()) {
            addSummaryBoxes(document, summaryMetrics);
            document.add(new Paragraph("\n"));
        }

        // 3. BREAKDOWN TABLES
        if (rows != null && !rows.isEmpty()) {
            addSectionHeader(document, "DETAILED BREAKDOWN");
            addBreakdownTable(document, columns, rows);
        }
    }

    public void generateFullSalesReport(
            String dateFrom,
            String dateTo,
            String generatedBy,
            List<Map<String, String>> metrics,
            List<String[]> timeData,
            boolean isHourly,
            List<String[]> topItems,
            List<String[]> categories,
            String outputPath) throws Exception {

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        writer.setPageEvent(new HeaderFooterEvent("Comprehensive Sales Report"));
        
        document.open();
        document.add(new Paragraph("\n"));

        addMetaSection(document, "Comprehensive Sales Report", dateFrom, dateTo, generatedBy);
        document.add(new Paragraph("\n"));

        if (metrics != null) {
            addSectionHeader(document, "KEY PERFORMANCE INDICATORS");
            addSummaryBoxes(document, metrics);
        }

        if (timeData != null && !timeData.isEmpty()) {
            document.add(new Paragraph("\n"));
            if (isHourly) {
                addSectionHeader(document, "HOURLY REVENUE FLOW");
                addDataTable(document, new String[]{"Time Block", "Revenue", "Orders"}, timeData);
            } else {
                addSectionHeader(document, "DAILY PERFORMANCE OVERVIEW");
                addDataTable(document, new String[]{"Date", "Revenue", "Orders"}, timeData);
            }
        }

        if (topItems != null && !topItems.isEmpty()) {
            document.add(new Paragraph("\n"));
            addSectionHeader(document, "TOP SELLING PRODUCTS");
            addDataTable(document, new String[]{"Item Name", "Qty Sold", "Total Sales"}, topItems);
        }

        if (categories != null && !categories.isEmpty()) {
            document.add(new Paragraph("\n"));
            addSectionHeader(document, "CATEGORY PERFORMANCE");
            addDataTable(document, new String[]{"Category", "Items Sold", "Revenue"}, categories);
        }

        document.close();
    }

    private void addSectionHeader(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY));
        p.setSpacingBefore(10);
        p.setSpacingAfter(10);
        document.add(p);
    }

    private void addDataTable(Document document, String[] headers, List<String[]> data) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h.toUpperCase(), fontTableHeader));
            c.setBackgroundColor(brandColor);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }

        int rowCount = 0;
        for (String[] row : data) {
            Color bgColor = (rowCount % 2 == 0) ? Color.WHITE : altRowColor;
            for (String val : row) {
                String displayVal = val;
                // Auto-peso for currency-like strings
                if (val.matches("\\d+\\.\\d{2}")) displayVal = "₱" + val;
                
                PdfPCell c = new PdfPCell(new Phrase(displayVal, fontBody));
                c.setBackgroundColor(bgColor);
                c.setPadding(8);
                c.setBorder(Rectangle.NO_BORDER);
                table.addCell(c);
            }
            rowCount++;
        }
        document.add(table);
    }

    private void addMetaSection(Document document, String title, String from, String to, String user) throws DocumentException {
        Paragraph pTitle = new Paragraph(title.toUpperCase(), fontTitle);
        pTitle.setSpacingAfter(10);
        document.add(pTitle);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        String genAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
        
        metaTable.addCell(createMetaCell("Report Range:", from + " to " + to));
        metaTable.addCell(createMetaCell("Generated By:", user));
        metaTable.addCell(createMetaCell("Generated At:", genAt));
        metaTable.addCell(new PdfPCell(new Phrase(""))); // Empty cell for balance

        document.add(metaTable);
    }

    private PdfPCell createMetaCell(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", fontLabel));
        p.add(new Chunk(value, fontBody));
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5);
        return cell;
    }

    private void addSummaryBoxes(Document document, List<Map<String, String>> metrics) throws DocumentException {
        PdfPTable table = new PdfPTable(metrics.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        for (Map<String, String> metric : metrics) {
            String label = metric.getOrDefault("label", "Metric");
            String value = metric.getOrDefault("value", "0");
            
            // Format currency if it's a financial metric
            if (label.toLowerCase().contains("revenue") || label.toLowerCase().contains("sales") || label.toLowerCase().contains("value")) {
                if (!value.startsWith("₱")) value = "₱" + value;
            }

            PdfPCell cell = new PdfPCell();
            cell.setPadding(12);
            cell.setBackgroundColor(altRowColor);
            cell.setBorderColor(Color.LIGHT_GRAY);
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph l = new Paragraph(label.toUpperCase(), fontLabel);
            l.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(l);

            Paragraph v = new Paragraph(value, fontMetric);
            v.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(v);

            table.addCell(cell);
        }
        document.add(table);
    }

    private void addBreakdownTable(Document document, List<String> columns, List<Map<String, String>> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        // Header
        for (String col : columns) {
            PdfPCell headerCell = new PdfPCell(new Phrase(col.toUpperCase(), fontTableHeader));
            headerCell.setBackgroundColor(brandColor);
            headerCell.setPadding(8);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(headerCell);
        }

        // Data Rows
        int rowCount = 0;
        for (Map<String, String> rowData : rows) {
            Color bgColor = (rowCount % 2 == 0) ? Color.WHITE : altRowColor;
            for (String col : columns) {
                String value = rowData.getOrDefault(col, "");
                
                // Format currency in table if detected
                if (col.toLowerCase().contains("total") || col.toLowerCase().contains("price") || col.toLowerCase().contains("revenue")) {
                    if (!value.startsWith("₱") && value.matches("\\d+\\.?\\d*")) {
                        value = "₱" + value;
                    }
                }

                PdfPCell cell = new PdfPCell(new Phrase(value, fontBody));
                cell.setBackgroundColor(bgColor);
                cell.setPadding(8);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            rowCount++;
        }

        document.add(table);
    }
}
