package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.subscription.PeriodSource;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTariffRepository subscriptionTariffRepository;
    private final BotService botService;
    private final UserService userService;

    public AdminSubscriptionController(SubscriptionService subscriptionService,
                                       SubscriptionRepository subscriptionRepository,
                                       SubscriptionTariffRepository subscriptionTariffRepository,
                                       BotService botService,
                                       UserService userService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionTariffRepository = subscriptionTariffRepository;
        this.botService = botService;
        this.userService = userService;
    }

    @GetMapping("/admin/subscriptions")
    public String list(@RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "tariffId", required = false) Long tariffId,
                       Model model) {
        List<SubscriptionEntity> all = subscriptionRepository.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionEntity::getId).reversed())
                .toList();
        SubscriptionStatus statusFilter = parseStatus(status);
        List<SubscriptionEntity> filtered = all.stream()
                .filter(s -> statusFilter == null || s.getStatus() == statusFilter)
                .filter(s -> tariffId == null || tariffId.equals(s.getTariffId()))
                .toList();
        model.addAttribute("subscriptions", filtered);
        model.addAttribute("statuses", SubscriptionStatus.values());

        Map<Long, SubscriptionTariffEntity> tariffById = botService.findAll().stream()
                .flatMap(b -> subscriptionTariffRepository.findByBotIdOrderBySortOrderAscIdAsc(b.getId()).stream())
                .collect(Collectors.toMap(SubscriptionTariffEntity::getId, t -> t, (a, b) -> a));
        List<SubscriptionTariffEntity> allTariffs = tariffById.values().stream()
                .sorted(Comparator.<SubscriptionTariffEntity, Long>comparing(SubscriptionTariffEntity::getBotId)
                        .thenComparingInt(SubscriptionTariffEntity::getSortOrder))
                .toList();

        Map<Long, String> tariffLabels = tariffById.values().stream()
                .collect(Collectors.toMap(SubscriptionTariffEntity::getId,
                        t -> t.getCode() + ": " + t.getTitle(),
                        (a, b) -> a));

        model.addAttribute("bots", botService.findAll().stream()
                .sorted(Comparator.comparing(BotEntity::getId)).toList());
        model.addAttribute("tariffs", allTariffs);
        model.addAttribute("tariffLabels", tariffLabels);
        model.addAttribute("periodSources", PeriodSource.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedTariffId", tariffId);
        return "admin/subscriptions";
    }

    @GetMapping("/admin/subscriptions/{id}")
    public String details(@PathVariable Long id, Model model) {
        SubscriptionEntity subscription = subscriptionService.findById(id).orElse(null);
        if (subscription == null) {
            return "redirect:/admin/subscriptions";
        }
        List<SubscriptionPeriodEntity> periods = subscriptionService.findPeriods(id);
        model.addAttribute("subscription", subscription);
        model.addAttribute("periods", periods);
        model.addAttribute("periodSources", PeriodSource.values());
        model.addAttribute("isActive", subscriptionService.isActive(subscription));
        userService.findById(subscription.getOwnerUserId()).ifPresent(u -> model.addAttribute("ownerUser", u));
        BotEntity bot = botService.findById(subscription.getBotId());
        model.addAttribute("bot", bot);
        subscriptionTariffRepository.findById(subscription.getTariffId()).ifPresent(t -> model.addAttribute("tariff", t));
        return "admin/subscription-details";
    }

    @PostMapping("/admin/subscriptions/create")
    public String create(@RequestParam Long ownerUserId,
                         @RequestParam Long botId,
                         @RequestParam Long tariffId,
                         @RequestParam(required = false) Long scopeChatId,
                         RedirectAttributes redirectAttributes) {
        try {
            Long scopeResolved = null;
            if (subscriptionTariffRepository.findById(tariffId).map(t -> t.getScope().isGroup()).orElse(false)) {
                scopeResolved = scopeChatId;
            }
            SubscriptionEntity created = subscriptionService.createOrGet(ownerUserId, botId, tariffId, scopeResolved);
            redirectAttributes.addFlashAttribute("success",
                    "Подписка #" + created.getId() + " создана/найдена");
            return "redirect:/admin/subscriptions/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/subscriptions";
        }
    }

    @PostMapping("/admin/subscriptions/{id}/grant-trial")
    public String grantTrial(@PathVariable Long id,
                             @RequestParam(defaultValue = "3") int days,
                             RedirectAttributes redirectAttributes) {
        try {
            SubscriptionEntity sub = subscriptionService.requireById(id);
            subscriptionService.activateTrial(sub, days, null);
            redirectAttributes.addFlashAttribute("success", "Триал на " + days + " дней активирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/subscriptions/" + id;
    }

    @PostMapping("/admin/subscriptions/{id}/extend")
    public String extend(@PathVariable Long id,
                         @RequestParam int months,
                         @RequestParam(defaultValue = "MANUAL_GRANT") String source,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirectAttributes) {
        try {
            SubscriptionEntity sub = subscriptionService.requireById(id);
            subscriptionService.extendMonths(sub, months, PeriodSource.valueOf(source), null, note);
            redirectAttributes.addFlashAttribute("success", "Продлено на " + months + " мес.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/subscriptions/" + id;
    }

    @PostMapping("/admin/subscriptions/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SubscriptionEntity sub = subscriptionService.requireById(id);
            subscriptionService.cancel(sub);
            redirectAttributes.addFlashAttribute("success", "Подписка отменена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/subscriptions/" + id;
    }

    @PostMapping("/admin/subscriptions/{id}/set-max")
    public String setMax(@PathVariable Long id,
                         @RequestParam(required = false) Integer maxParticipants,
                         RedirectAttributes redirectAttributes) {
        try {
            SubscriptionEntity sub = subscriptionService.requireById(id);
            subscriptionService.setMaxParticipants(sub, maxParticipants);
            redirectAttributes.addFlashAttribute("success", "Лимит участников обновлён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/subscriptions/" + id;
    }

    private static SubscriptionStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try { return SubscriptionStatus.valueOf(value); } catch (IllegalArgumentException e) { return null; }
    }
}
