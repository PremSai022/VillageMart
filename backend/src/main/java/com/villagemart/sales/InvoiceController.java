package com.villagemart.sales;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sales")
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;
    private final SaleService saleService;

    @Operation(summary = "Download the invoice PDF (inline=true to display instead of download)")
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean inline) {
        var sale = saleService.get(id);
        byte[] pdf = invoicePdfService.render(sale);
        String fileName = "invoice-" + sale.invoiceNumber() + ".pdf";
        ContentDisposition disposition = inline
                ? ContentDisposition.inline().filename(fileName).build()
                : ContentDisposition.attachment().filename(fileName).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(pdf.length)
                .body(pdf);
    }
}
