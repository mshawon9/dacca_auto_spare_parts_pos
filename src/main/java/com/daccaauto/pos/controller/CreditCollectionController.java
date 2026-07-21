package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.service.CreditCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

@Controller
@RequestMapping("/credit-collections")
@RequiredArgsConstructor
public class CreditCollectionController {

    private static final int PAGE_SIZE = 15;

    private final CreditCollectionService creditCollectionService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long customerId,
                       @RequestParam(defaultValue = "all") String collectionType,
                       @RequestParam(required = false) String statementMonth,
                       @RequestParam(defaultValue = "false") boolean overdueOnly,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        YearMonth selectedMonth = parseMonth(statementMonth);
        var creditPage = creditCollectionService.search(
            keyword,
            customerId,
            collectionType,
            overdueOnly,
            PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        model.addAttribute("creditPage", creditPage);
        model.addAttribute("credits", creditPage.getContent());
        model.addAttribute("summary", creditCollectionService.summarize(keyword, customerId, collectionType, overdueOnly));
        model.addAttribute("keyword", keyword);
        model.addAttribute("customerId", customerId);
        model.addAttribute("collectionType", collectionType);
        model.addAttribute("statementMonth", selectedMonth.toString());
        model.addAttribute("monthlySummary", creditCollectionService.monthlyStatementSummary(customerId, selectedMonth));
        model.addAttribute("overdueOnly", overdueOnly);
        model.addAttribute("customers", customerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("paymentMethods", Arrays.stream(PaymentMethod.values())
            .filter(method -> method != PaymentMethod.CREDIT)
            .toList());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("pageTitle", "Credit Collection");
        return "credit-collection/list";
    }

    @PostMapping("/monthly/collect")
    public String collectMonthly(@RequestParam Long customerId,
                                 @RequestParam String statementMonth,
                                 @Valid @ModelAttribute CreditCollectionRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please check monthly collection amount and payment details.");
            return "redirect:/credit-collections?collectionType=monthly&customerId=" + customerId + "&statementMonth=" + statementMonth;
        }
        try {
            creditCollectionService.collectMonthlyStatement(customerId, parseMonth(statementMonth), request);
            redirectAttributes.addFlashAttribute("successMessage", "Monthly statement collection saved successfully.");
        } catch (DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/credit-collections?collectionType=monthly&customerId=" + customerId + "&statementMonth=" + statementMonth;
    }

    @PostMapping("/{saleId}/collect")
    public String collect(@PathVariable Long saleId,
                          @Valid @ModelAttribute CreditCollectionRequest request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please check collection amount and payment details.");
            return "redirect:/credit-collections";
        }
        try {
            creditCollectionService.collect(saleId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Credit collection saved successfully.");
        } catch (DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/credit-collections";
    }

    @PostMapping("/{saleId}/due-date")
    public String updateDueDate(@PathVariable Long saleId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                RedirectAttributes redirectAttributes) {
        creditCollectionService.updateDueDate(saleId, dueDate);
        redirectAttributes.addFlashAttribute("successMessage", "Due date updated successfully.");
        return "redirect:/credit-collections";
    }

    @GetMapping("/{saleId}/payments")
    @ResponseBody
    public Object payments(@PathVariable Long saleId) {
        return creditCollectionService.payments(saleId);
    }

    private YearMonth parseMonth(String statementMonth) {
        return statementMonth == null || statementMonth.isBlank() ? YearMonth.now() : YearMonth.parse(statementMonth);
    }
}
