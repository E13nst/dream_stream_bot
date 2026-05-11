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
import java.util.Objects;

@Controller
public class AdminConsentController {

    private final ConsentService consentService;

    public AdminConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/admin/consents")
    public String list(Model model) {
        List<ConsentDocumentEntity> docs = consentService.listLatestVersions().stream()
                .sorted(Comparator.comparing(ConsentDocumentEntity::getCode))
                .toList();
        model.addAttribute("documents", docs);
        model.addAttribute("codes", ConsentCode.values());
        model.addAttribute("changeTypes", ConsentChangeType.values());
        return "admin/consents";
    }

    @GetMapping("/admin/consents/{id}")
    public String details(@PathVariable Long id,
                          @RequestParam(required = false) Integer version,
                          Model model) {
        ConsentDocumentEntity base;
        try {
            base = consentService.requireDocument(id);
        } catch (Exception e) {
            return "redirect:/admin/consents";
        }
        ConsentDocumentEntity doc = base;
        if (version != null) {
            doc = consentService.listVersions(base.getCode()).stream()
                    .filter(d -> Objects.equals(d.getVersion(), version))
                    .findFirst()
                    .orElse(base);
        }
        model.addAttribute("document", doc);
        model.addAttribute("versions", consentService.listVersions(doc.getCode()));
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
            ConsentDocumentEntity source = consentService.requireDocument(id);
            ConsentChangeType requestedChangeType = ConsentChangeType.valueOf(changeType);
            String requestedTitle = title != null ? title : source.getTitle();
            String requestedBody = bodyMarkdown != null ? bodyMarkdown : source.getBodyMarkdown();
            String requestedExternalUrl = externalUrl != null ? externalUrl : source.getExternalUrl();

            boolean changed = !Objects.equals(source.getTitle(), requestedTitle)
                    || !Objects.equals(source.getBodyMarkdown(), requestedBody)
                    || !Objects.equals(source.getExternalUrl(), requestedExternalUrl)
                    || source.getChangeType() != requestedChangeType;

            ConsentDocumentEntity toPublish = source;
            if (changed) {
                toPublish = consentService.createDraftFrom(
                        source.getId(),
                        requestedTitle,
                        requestedBody,
                        requestedExternalUrl,
                        requestedChangeType
                );
            }
            ConsentDocumentEntity published = consentService.publish(toPublish.getId(), toTelegraph);
            redirectAttributes.addFlashAttribute("success",
                    changed ? "Создана новая версия и опубликована" : "Документ опубликован");
            return "redirect:/admin/consents/" + published.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/consents/" + id;
    }

    @PostMapping("/admin/consents/{id}/new-version")
    public String createNewVersionFrom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ConsentDocumentEntity source = consentService.requireDocument(id);
            ConsentDocumentEntity draft = consentService.createDraftFrom(
                    source.getId(),
                    source.getTitle(),
                    source.getBodyMarkdown(),
                    source.getExternalUrl(),
                    source.getChangeType()
            );
            redirectAttributes.addFlashAttribute("success", "Создан новый черновик версии v" + draft.getVersion());
            return "redirect:/admin/consents/" + draft.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/consents/" + id;
        }
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
