package com.daccaauto.pos.controller;

import com.daccaauto.pos.service.PurchaseInvoicePhoneService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/purchase-orders/invoice/phone")
public class PurchaseInvoicePhoneController {
    private final PurchaseInvoicePhoneService service;

    public PurchaseInvoicePhoneController(PurchaseInvoicePhoneService service) {
        this.service = service;
    }

    @PostMapping("/connections")
    @ResponseBody
    public ResponseEntity<PurchaseInvoicePhoneService.Connection> connect(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.connect(request.getSession(true).getId()));
    }

    @GetMapping("/connections/{token}")
    @ResponseBody
    public ResponseEntity<byte[]> get(@PathVariable String token, HttpServletRequest request) {
        var photo = service.get(token, owner(request));
        if (photo == null) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(photo.contentType())).body(photo.bytes());
    }

    @DeleteMapping("/connections/{token}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable String token, HttpServletRequest request) {
        service.delete(token, owner(request));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping(value = "/connections/{token}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Void> upload(@PathVariable String token, @RequestParam("file") MultipartFile file) {
        service.upload(token, file);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/mobile")
    public String mobile(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        return "purchase-order/invoice-phone";
    }

    private static String owner(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getId();
    }
}
