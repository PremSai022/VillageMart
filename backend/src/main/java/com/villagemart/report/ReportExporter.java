package com.villagemart.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.villagemart.common.ApiException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Renders {@link ReportTable}s as PDF (OpenPDF) or XLSX (Apache POI). */
@Component
public class ReportExporter {

    private static final Color HEADER_BG = new Color(0x0F, 0x2A, 0x44);
    private static final Color STRIPE = new Color(0xF3, 0xF5, 0xF8);

    public byte[] pdf(ReportTable table, String businessName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(table.columns().size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 28, 28);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font brand = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(0x55, 0x60, 0x70));
            Font title = new Font(Font.HELVETICA, 16, Font.BOLD, HEADER_BG);
            Font sub = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            Font head = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
            Font body = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
            Font bold = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);

            doc.add(new Paragraph(businessName, brand));
            doc.add(new Paragraph(table.title(), title));
            doc.add(new Paragraph(table.subtitle() + "   |   Amounts in INR (Rs.)   |   Generated "
                    + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), sub));
            doc.add(new Paragraph(" "));

            if (!table.summary().isEmpty()) {
                PdfPTable summary = new PdfPTable(2);
                summary.setWidthPercentage(55);
                summary.setHorizontalAlignment(Element.ALIGN_LEFT);
                for (String[] s : table.summary()) {
                    summary.addCell(plainCell(s[0], body, Element.ALIGN_LEFT, null));
                    summary.addCell(plainCell(s[1], bold, Element.ALIGN_RIGHT, null));
                }
                doc.add(summary);
                doc.add(new Paragraph(" "));
            }

            if (!table.columns().isEmpty()) {
                PdfPTable grid = new PdfPTable(table.columns().size());
                grid.setWidthPercentage(100);
                grid.setHeaderRows(1);
                for (ReportTable.Column c : table.columns()) {
                    PdfPCell cell = plainCell(c.header(), head, align(c.type()), HEADER_BG);
                    grid.addCell(cell);
                }
                if (table.rows().isEmpty()) {
                    PdfPCell empty = plainCell("No data for the selected period", body, Element.ALIGN_CENTER, null);
                    empty.setColspan(table.columns().size());
                    grid.addCell(empty);
                }
                int i = 0;
                for (var row : table.rows()) {
                    Color bg = i++ % 2 == 1 ? STRIPE : null;
                    for (int c = 0; c < row.size(); c++) {
                        ReportTable.Column col = table.columns().get(c);
                        grid.addCell(plainCell(ReportFormat.cell(row.get(c), col.type()), body, align(col.type()), bg));
                    }
                }
                doc.add(grid);
            }
        } catch (DocumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_FAILED", "Could not generate PDF: " + e.getMessage());
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    public byte[] xlsx(ReportTable table, String businessName) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(safeSheetName(table.title()));
            var titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            var headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setBorderBottom(BorderStyle.THIN);
            var fmt = wb.createDataFormat();
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(fmt.getFormat("#,##0.00"));
            CellStyle qty = wb.createCellStyle();
            qty.setDataFormat(fmt.getFormat("#,##0.###"));
            CellStyle integer = wb.createCellStyle();
            integer.setDataFormat(fmt.getFormat("0"));
            var boldFont = wb.createFont();
            boldFont.setBold(true);
            CellStyle bold = wb.createCellStyle();
            bold.setFont(boldFont);

            int r = 0;
            sheet.createRow(r++).createCell(0).setCellValue(businessName);
            Cell t = sheet.createRow(r++).createCell(0);
            t.setCellValue(table.title());
            t.setCellStyle(titleStyle);
            sheet.createRow(r++).createCell(0).setCellValue(table.subtitle() + " | Amounts in INR");
            r++;
            for (String[] s : table.summary()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s[0]);
                Cell v = row.createCell(1);
                v.setCellValue(s[1]);
                v.setCellStyle(bold);
            }
            if (!table.summary().isEmpty()) {
                r++;
            }
            int[] widths = new int[Math.max(table.columns().size(), 2)];
            widths[0] = 20;
            widths[1] = 16;
            if (!table.columns().isEmpty()) {
                Row header = sheet.createRow(r++);
                for (int c = 0; c < table.columns().size(); c++) {
                    Cell cell = header.createCell(c);
                    cell.setCellValue(table.columns().get(c).header());
                    cell.setCellStyle(headStyle);
                    widths[c] = Math.max(widths[c], table.columns().get(c).header().length() + 2);
                }
                for (var values : table.rows()) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < values.size(); c++) {
                        Object v = values.get(c);
                        Cell cell = row.createCell(c);
                        ReportTable.ColumnType type = table.columns().get(c).type();
                        if (v instanceof BigDecimal b) {
                            cell.setCellValue(b.doubleValue());
                            cell.setCellStyle(type == ReportTable.ColumnType.QUANTITY ? qty : money);
                        } else if (v instanceof Number n) {
                            cell.setCellValue(n.longValue());
                            cell.setCellStyle(integer);
                        } else if (v != null) {
                            cell.setCellValue(v.toString());
                        }
                        widths[c] = Math.max(widths[c], Math.min(60, ReportFormat.cell(v, type).length() + 2));
                    }
                }
            }
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c] * 256);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_FAILED", "Could not generate Excel file: " + e.getMessage());
        }
    }

    private static String safeSheetName(String title) {
        String s = title.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return s.length() > 31 ? s.substring(0, 31) : s;
    }

    private static int align(ReportTable.ColumnType type) {
        return type == ReportTable.ColumnType.TEXT ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT;
    }

    private static PdfPCell plainCell(String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        cell.setBorderColor(new Color(0xD9, 0xDE, 0xE5));
        if (bg != null) {
            cell.setBackgroundColor(bg);
        }
        return cell;
    }
}
