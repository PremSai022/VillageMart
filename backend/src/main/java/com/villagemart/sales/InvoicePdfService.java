package com.villagemart.sales;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.villagemart.common.ApiException;
import com.villagemart.payment.PaymentDirection;
import com.villagemart.payment.PaymentDtos.PaymentDto;
import com.villagemart.sales.SaleDtos.SaleDto;
import com.villagemart.sales.SaleDtos.SaleItemDto;
import com.villagemart.settings.BusinessSettingsDtos.BusinessSettingsDto;
import com.villagemart.settings.BusinessSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

/** Generates a printable/shareable A5 PDF invoice from the immutable sale snapshot. */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final Color BRAND = new Color(0x0F, 0x2A, 0x44);
    private static final Color MUTED = new Color(0x5B, 0x66, 0x75);
    private static final Color LINE = new Color(0xD9, 0xDE, 0xE5);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final SaleService saleService;
    private final BusinessSettingsService settingsService;

    public byte[] render(Long saleId) {
        return render(saleService.get(saleId));
    }

    public byte[] render(SaleDto sale) {
        return render(sale, settingsService.get());
    }

    public byte[] render(SaleDto sale, BusinessSettingsDto biz) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font name = new Font(Font.HELVETICA, 15, Font.BOLD, BRAND);
            Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
            Font label = new Font(Font.HELVETICA, 8, Font.BOLD, MUTED);
            Font body = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
            Font bold = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.BLACK);
            Font head = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font total = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND);
            Font stamp = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0xC6, 0x28, 0x28));

            // Header: business identity and invoice meta.
            PdfPTable header = new PdfPTable(new float[]{3, 2});
            header.setWidthPercentage(100);
            PdfPCell left = borderless();
            left.addElement(new Paragraph(biz.businessName(), name));
            if (notBlank(biz.address())) {
                left.addElement(new Paragraph(biz.address(), small));
            }
            if (notBlank(biz.mobile())) {
                left.addElement(new Paragraph("Mobile: " + biz.mobile(), small));
            }
            if (biz.gstEnabled() && notBlank(biz.gstin())) {
                left.addElement(new Paragraph("GSTIN: " + biz.gstin(), small));
            }
            header.addCell(left);
            PdfPCell right = borderless();
            Paragraph title = new Paragraph(biz.gstEnabled() ? "TAX INVOICE" : "INVOICE", new Font(Font.HELVETICA, 12, Font.BOLD, BRAND));
            title.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(title);
            right.addElement(rightAligned("No: " + sale.invoiceNumber(), bold));
            right.addElement(rightAligned("Date: " + sale.saleDate().format(DATE), body));
            if (sale.status() != SaleStatus.COMPLETED) {
                right.addElement(rightAligned(sale.status().name().replace('_', ' '), stamp));
            }
            header.addCell(right);
            doc.add(header);
            doc.add(rule());

            // Bill-to block.
            PdfPTable billTo = new PdfPTable(new float[]{1, 1});
            billTo.setWidthPercentage(100);
            PdfPCell customer = borderless();
            customer.addElement(new Paragraph("BILL TO", label));
            if (sale.customerId() != null) {
                customer.addElement(new Paragraph(sale.customerName(), bold));
                if (notBlank(sale.customerMobile())) {
                    customer.addElement(new Paragraph("Mobile: " + sale.customerMobile(), body));
                }
            } else {
                customer.addElement(new Paragraph("Walk-in customer", body));
            }
            billTo.addCell(customer);
            PdfPCell village = borderless();
            village.addElement(new Paragraph("VILLAGE", label));
            village.addElement(new Paragraph(notBlank(sale.villageName()) ? sale.villageName() : "-", body));
            billTo.addCell(village);
            doc.add(billTo);
            doc.add(new Paragraph(" ", small));

            // Line items.
            boolean tax = sale.taxAmount() != null && sale.taxAmount().signum() > 0;
            PdfPTable items = tax
                    ? new PdfPTable(new float[]{0.5f, 4, 1.3f, 1.4f, 1.2f, 1.1f, 1.7f})
                    : new PdfPTable(new float[]{0.5f, 4.5f, 1.4f, 1.5f, 1.2f, 1.8f});
            items.setWidthPercentage(100);
            items.setHeaderRows(1);
            addHead(items, head, "#", "Item", "Qty", "Rate", "Disc");
            if (tax) {
                addHead(items, head, "GST %");
            }
            addHead(items, head, "Amount");
            int n = 1;
            for (SaleItemDto i : sale.items()) {
                items.addCell(cell(String.valueOf(n++), body, Element.ALIGN_LEFT));
                items.addCell(cell(i.productName(), body, Element.ALIGN_LEFT));
                items.addCell(cell(qty(i.quantity()) + " " + i.unitCode(), body, Element.ALIGN_RIGHT));
                items.addCell(cell(money(i.unitPrice()), body, Element.ALIGN_RIGHT));
                items.addCell(cell(i.discountAmount().signum() > 0 ? money(i.discountAmount()) : "-", body, Element.ALIGN_RIGHT));
                if (tax) {
                    items.addCell(cell(i.taxRate() == null ? "-" : i.taxRate().stripTrailingZeros().toPlainString(), body, Element.ALIGN_RIGHT));
                }
                items.addCell(cell(money(i.lineTotal()), body, Element.ALIGN_RIGHT));
            }
            doc.add(items);

            // Totals.
            PdfPTable totals = new PdfPTable(new float[]{3, 2});
            totals.setWidthPercentage(60);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingBefore(6);
            totalRow(totals, "Subtotal", sale.subtotal(), body);
            if (sale.itemDiscountTotal().signum() > 0) {
                totalRow(totals, "Item discounts", sale.itemDiscountTotal().negate(), body);
            }
            if (sale.billDiscount().signum() > 0) {
                totalRow(totals, "Bill discount", sale.billDiscount().negate(), body);
            }
            if (tax) {
                totalRow(totals, "Taxable value", sale.taxableAmount(), body);
                totalRow(totals, "GST", sale.taxAmount(), body);
            }
            if (sale.roundOff().signum() != 0) {
                totalRow(totals, "Round off", sale.roundOff(), body);
            }
            totalRow(totals, "Grand total (Rs.)", sale.totalAmount(), total);
            if (sale.returnedAmount().signum() > 0) {
                totalRow(totals, "Returned", sale.returnedAmount().negate(), body);
            }
            totalRow(totals, "Amount paid", sale.paidAmount(), bold);
            totalRow(totals, "Balance due", sale.outstandingAmount(), bold);
            doc.add(totals);

            // Payments.
            String methods = sale.payments().stream()
                    .filter(p -> p.direction() == PaymentDirection.IN)
                    .map(this::describe)
                    .collect(Collectors.joining(", "));
            Paragraph pay = new Paragraph("Payment: " + sale.paymentMode().name().replace('_', ' ')
                    + (methods.isEmpty() ? "" : " (" + methods + ")"), body);
            pay.setSpacingBefore(8);
            doc.add(pay);
            if (biz.gstEnabled()) {
                doc.add(new Paragraph("Prices are exclusive of GST; tax shown separately.", small));
            }

            doc.add(rule());
            Paragraph thanks = new Paragraph(notBlank(biz.invoiceFooter()) ? biz.invoiceFooter()
                    : "Thank you for shopping with us! Visit again.", new Font(Font.HELVETICA, 9, Font.ITALIC, BRAND));
            thanks.setAlignment(Element.ALIGN_CENTER);
            doc.add(thanks);
        } catch (DocumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVOICE_RENDER_FAILED", "Could not generate invoice: " + e.getMessage());
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private String describe(PaymentDto p) {
        return p.method().name() + " " + money(p.amount()) + (p.reversed() ? " reversed" : "");
    }

    private static void addHead(PdfPTable t, Font f, String... headers) {
        for (String h : headers) {
            PdfPCell c = cell(h, f, h.equals("#") || h.equals("Item") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            c.setBackgroundColor(BRAND);
            t.addCell(c);
        }
    }

    private static void totalRow(PdfPTable t, String label, BigDecimal value, Font f) {
        PdfPCell l = cell(label, f, Element.ALIGN_LEFT);
        l.setBorder(Rectangle.NO_BORDER);
        PdfPCell v = cell(money(value), f, Element.ALIGN_RIGHT);
        v.setBorder(Rectangle.NO_BORDER);
        t.addCell(l);
        t.addCell(v);
    }

    private static PdfPCell cell(String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, f));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(LINE);
        return c;
    }

    private static PdfPCell borderless() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static Paragraph rightAligned(String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static Paragraph rule() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(LINE);
        c.setFixedHeight(6);
        t.addCell(c);
        Paragraph p = new Paragraph();
        p.add(t);
        p.setSpacingAfter(6);
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String money(BigDecimal v) {
        return v == null ? "" : new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }

    private static String qty(BigDecimal v) {
        return v == null ? "" : new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }
}
