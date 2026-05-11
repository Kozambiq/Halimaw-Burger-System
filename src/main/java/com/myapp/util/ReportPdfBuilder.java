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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for building professional PDF reports using the OpenPDF library.
 * Provides a standardized layout for all system exports, including 
 * brand-consistent styling, automated headers/footers, and KPI summaries.
 */
public class ReportPdfBuilder {
    // BRANDING CONSTANTS: Used to maintain visual identity across all reports
    private final Color brandColor = new Color(180, 50, 30);
    private final Color altRowColor = new Color(245, 245, 245);
    private final Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, brandColor);
    private final Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private final Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private final Font fontMetric = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, brandColor);
    private final Font fontTableHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

    /**
     * Orchestrates the construction of a complete report.
     * FLOW: Initialize Document -> Add Metadata -> Add Summary KPIs -> Add Main Data Table -> Close.
     */
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
        
        // EVENT HOOK: Automatically adds the brand header and page numbers on every page
        writer.setPageEvent(new HeaderFooterEvent(reportTitle));
        
        document.open();
        document.add(new Paragraph("\n")); // Spacer for header

        // 1. META SECTION: Report identification details
        addMetaSection(document, reportTitle, dateFrom, dateTo, generatedBy);
        document.add(new Paragraph("\n"));

        // 2. SUMMARY BOXES: Highlights key KPIs (e.g., Total Revenue, Order Count)
        if (summaryMetrics != null && !summaryMetrics.isEmpty()) {
            addSummaryBoxes(document, summaryMetrics);
            document.add(new Paragraph("\n"));
        }

        // 3. BREAKDOWN TABLES: Detailed itemized list of records
        if (rows != null && !rows.isEmpty()) {
            addSectionHeader(document, "DETAILED BREAKDOWN");
            addBreakdownTable(document, columns, rows);
        }
    }

    /**
     * Specialized generator for the comprehensive Sales Report.
     * Includes time-series data, top items, and category breakdowns in a single document.
     */
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

        // KPI SECTION
        if (metrics != null) {
            addSectionHeader(document, "KEY PERFORMANCE INDICATORS");
            addSummaryBoxes(document, metrics);
        }

        // TREND ANALYSIS: Hourly or Daily revenue flow
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

        // PRODUCT PERFORMANCE: Most popular items
        if (topItems != null && !topItems.isEmpty()) {
            document.add(new Paragraph("\n"));
            addSectionHeader(document, "TOP SELLING PRODUCTS");
            addDataTable(document, new String[]{"Item Name", "Qty Sold", "Total Sales"}, topItems);
        }

        // CATEGORY PERFORMANCE: Revenue split by product type
        if (categories != null && !categories.isEmpty()) {
            document.add(new Paragraph("\n"));
            addSectionHeader(document, "CATEGORY PERFORMANCE");
            addDataTable(document, new String[]{"Category", "Items Sold", "Revenue"}, categories);
        }

        document.close();
    }

    /**
     * Adds a bold subtitle header to divide different report sections.
     */
    private void addSectionHeader(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY));
        p.setSpacingBefore(10);
        p.setSpacingAfter(10);
        document.add(p);
    }

    /**
     * Localized currency formatter for the Philippines (₱).
     * Handles cleaning of raw strings before parsing to numeric values.
     */
    private String formatCurrency(String value) {
        if (value == null || value.isEmpty() || "No sales yet".equalsIgnoreCase(value) || "N/A".equalsIgnoreCase(value)) {
            return value;
        }
        try {
            // Remove existing symbol or commas if any
            String cleanValue = value.replace("₱", "").replace(",", "").trim();
            double amount = Double.parseDouble(cleanValue);
            NumberFormat phFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            return phFormat.format(amount);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Generic table builder for data represented as String arrays.
     * Implements brand styling for headers and Zebra-striping for rows.
     */
    private void addDataTable(Document document, String[] headers, List<String[]> data) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setKeepTogether(true);

        // STYLED HEADER
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h.toUpperCase(), fontTableHeader));
            c.setBackgroundColor(brandColor);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }

        // ZEBRA-STRIPED ROWS
        int rowCount = 0;
        for (String[] row : data) {
            Color bgColor = (rowCount % 2 == 0) ? Color.WHITE : altRowColor;
            for (int i = 0; i < row.length; i++) {
                String val = row[i];
                String displayVal = val;
                
                // AUTO-CURRENCY DETECTION: Formats values if header implies financial data
                String header = headers[i].toLowerCase();
                if (header.contains("revenue") || header.contains("sales") || header.contains("price") || val.matches("\\d+\\.\\d{2}")) {
                    displayVal = formatCurrency(val);
                }
                
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

    /**
     * Adds the report metadata (date range, user, timestamp) in a grid layout.
     */
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

    /**
     * Adds standardized summary KPI boxes (highlights).
     */
    private void addSummaryBoxes(Document document, List<Map<String, String>> metrics) throws DocumentException {
        PdfPTable table = new PdfPTable(metrics.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
        table.setKeepTogether(true);

        for (Map<String, String> metric : metrics) {
            String label = metric.getOrDefault("label", "Metric");
            String value = metric.getOrDefault("value", "0");
            
            // Format currency if it's a financial metric
            if (label.toLowerCase().contains("revenue") || label.toLowerCase().contains("sales") || label.toLowerCase().contains("value")) {
                value = formatCurrency(value);
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

    /**
     * Specialized Breakdown Table for Map-based data rows.
     */
    private void addBreakdownTable(Document document, List<String> columns, List<Map<String, String>> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setKeepTogether(true);

        // HEADER
        for (String col : columns) {
            PdfPCell headerCell = new PdfPCell(new Phrase(col.toUpperCase(), fontTableHeader));
            headerCell.setBackgroundColor(brandColor);
            headerCell.setPadding(8);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(headerCell);
        }

        // DATA ROWS
        int rowCount = 0;
        for (Map<String, String> rowData : rows) {
            Color bgColor = (rowCount % 2 == 0) ? Color.WHITE : altRowColor;
            for (String col : columns) {
                String value = rowData.getOrDefault(col, "");
                
                // CURRENCY DETECTION
                if (col.toLowerCase().contains("total") || col.toLowerCase().contains("price") || col.toLowerCase().contains("revenue")) {
                    value = formatCurrency(value);
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
