package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.consent.ConsentChangeType;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.service.consent.ConsentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
public class AdminConsentController {

    private final ConsentService consentService;

    public AdminConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/admin/consents")
    public String list(Model model) {
        List<ConsentDocumentEntity> docs = consentService.listAll().stream()
                .sorted(Comparator.comparing(ConsentDocumentEntity::getCode)
                        .thenComparing(Comparator.comparing(ConsentDocumentEntity::getVersion).reversed()))
                .toList();
        model.addAttribute("documents", docs);
        model.addAttribute("codes", ConsentCode.values());
        model.addAttribute("changeTypes", ConsentChangeType.values());
        return "admin/consents";
    }

    @GetMapping("/admin/consents/{id}")
    public String details(@PathVariable Long id, Model model) {
        ConsentDocumentEntity doc = consentService.listAll().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (doc == null) {
            return "redirect:/admin/consents";
        }
        model.addAttribute("document", doc);
        model.addAttribute("changeTypes", ConsentChangeType.values());
        return "admin/consent-edit";
    }

    @PostMapping("/admin/consents/new")
    public String createDraft(@RequestParam String code,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) String bodyMarkdown,
                              @RequestParam(required = false) String externalUrl,
                              @RequestParam(defaultValue = "MINOR") String changeType,
                              RedirectAttributes redirectAttributes) {
        try {
            ConsentDocumentEntity draft = consentService.createDraft(
                    ConsentCode.valueOf(code),
                    title,
                    bodyMarkdown,
                    externalUrl,
                    ConsentChangeType.valueOf(changeType));
            redirectAttributes.addFlashAttribute("success", "Создана версия " + draft.getVersion());
            return "redirect:/admin/consents/" + draft.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/consents";
        }
    }

    @PostMapping("/admin/consents/{id}/publish")
    public String publish(@PathVariable Long id,
                          @RequestParam(name = "toTelegraph", defaultValue = "false") boolean toTelegraph,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) String bodyMarkdown,
                          @RequestParam(required = false) String externalUrl,
                          @RequestParam(defaultValue = "MINOR") String changeType,
                          RedirectAttributes redirectAttributes) {
        try {
            ConsentDocumentEntity doc = consentService.listAll().stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Документ не найден"));
            if (title != null) {
                doc.setTitle(title);
            }
            if (bodyMarkdown != null) {
                doc.setBodyMarkdown(bodyMarkdown);
            }
            if (externalUrl != null && !externalUrl.isBlank()) {
                doc.setExternalUrl(externalUrl);
            }
            doc.setChangeType(ConsentChangeType.valueOf(changeType));
            consentService.publish(doc.getId(), toTelegraph);
            redirectAttributes.addFlashAttribute("success", "Документ опубликован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/consents/" + id;
    }

    @PostMapping("/admin/consents/escalate")
    public String escalate(RedirectAttributes redirectAttributes) {
        try {
            int n = consentService.escalateExpiredConsents();
            redirectAttributes.addFlashAttribute("success",
                    "Эскалировано подписок в BLOCKED_CONSENT: " + n);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/consents";
    }
}
