package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.inventory.StockAdjustmentRequest;
import com.daccaauto.pos.entity.OrderTodoStatus;
import com.daccaauto.pos.entity.StockAdjustmentType;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.InventoryService;
import com.daccaauto.pos.service.OrderTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/order-todos")
@RequiredArgsConstructor
public class OrderTodoController {

    private static final int PAGE_SIZE = 15;

    private final OrderTodoService orderTodoService;
    private final InventoryService inventoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean showReorder,
                       Model model) {
        Page<?> todoPage = orderTodoService.getOpenTodos(keyword, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("todoPage", todoPage);
        model.addAttribute("todos", todoPage.getContent());
        model.addAttribute("reorderItems", showReorder ? orderTodoService.getReorderCandidates() : java.util.List.of());
        model.addAttribute("showReorder", showReorder);
        model.addAttribute("statuses", java.util.Arrays.stream(OrderTodoStatus.values())
            .filter(status -> status != OrderTodoStatus.CANCELLED)
            .toList());
        model.addAttribute("stores", inventoryService.getActiveStores());
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Order Todo List");
        return "order-todo/list";
    }

    @PostMapping("/create")
    public String create(@RequestParam Long productId,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirectAttributes) {
        orderTodoService.createManualTodo(productId, note);
        redirectAttributes.addFlashAttribute("successMessage", "Order todo added.");
        return "redirect:/order-todos";
    }

    @PostMapping("/add-reorder")
    public String addReorderItems(@RequestParam(required = false) java.util.List<Long> productIds,
                                  RedirectAttributes redirectAttributes) {
        if (productIds == null || productIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Select at least one reorder item to add.");
            return "redirect:/order-todos?showReorder=true";
        }
        orderTodoService.addSelectedReorderTodos(productIds);
        redirectAttributes.addFlashAttribute("successMessage", "Selected reorder items added to order todo.");
        return "redirect:/order-todos";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderTodoStatus status,
                               @RequestParam(required = false) String note,
                               RedirectAttributes redirectAttributes) {
        orderTodoService.updateStatus(id, status, note);
        redirectAttributes.addFlashAttribute("successMessage", "Order todo updated.");
        return "redirect:/order-todos";
    }

    @PostMapping("/stock")
    public String addStock(@RequestParam Long todoId,
                           @RequestParam Long storeId,
                           @RequestParam Long productId,
                           @RequestParam BigDecimal quantity,
                           RedirectAttributes redirectAttributes) {
        try {
            inventoryService.adjustStock(new StockAdjustmentRequest(
                storeId,
                productId,
                StockAdjustmentType.INCREASE,
                quantity,
                "Added from order todo"
            ));
            orderTodoService.markReceived(todoId);
            redirectAttributes.addFlashAttribute("successMessage", "Stock added successfully.");
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/order-todos";
    }

    @PostMapping("/refresh")
    public String refresh(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage", "Reorder item list refreshed.");
        return "redirect:/order-todos?showReorder=true";
    }
}
