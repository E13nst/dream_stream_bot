package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.subscription.PeriodSource;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPeriodEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPlan;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
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

@Controller
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final BotService botService;
    private final UserService userService;

    public AdminSubscriptionController(SubscriptionService subscriptionService,
                                       BotService botService,
                                       UserService userService) {
        this.subscriptionService = subscriptionService;
        this.botService = botService;
        this.userService = userService;
    }

    @GetMapping("/admin/subscriptions")
    public String list(@RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "plan", required = false) String plan,
                       Model model) {
        List<SubscriptionEntity> all = subscriptionService.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionEntity::getId).reversed())
                .toList();
        SubscriptionStatus statusFilter = parseStatus(status);
        SubscriptionPlan planFilter = parsePlan(plan);
        List<SubscriptionEntity> filtered = all.stream()
                .filter(s -> statusFilter == null || s.getStatus() == statusFilter)
                .filter(s -> planFilter == null || s.getPlan() == planFilter)
                .toList();
        model.addAttribute("subscriptions", filtered);
        model.addAttribute("plans", SubscriptionPlan.values());
        model.addAttribute("statuses", SubscriptionStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPlan", plan);
        model.addAttribute("bots", botService.findAll().stream()
                .sorted(Comparator.comparing(BotEntity::getId))
                .toList());
        model.addAttribute("periodSources", PeriodSource.values());
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
        return "admin/subscription-details";
    }

    @PostMapping("/admin/subscriptions/create")
    public String create(@RequestParam Long ownerUserId,
                         @RequestParam Long botId,
                         @RequestParam String plan,
                         @RequestParam(required = false) Long scopeChatId,
                         RedirectAttributes redirectAttributes) {
        try {
            SubscriptionPlan planValue = SubscriptionPlan.valueOf(plan);
            SubscriptionEntity created = subscriptionService.createOrGet(
                    ownerUserId, botId, planValue,
                    planValue.isGroup() ? scopeChatId : null);
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

    private static SubscriptionPlan parsePlan(String value) {
        if (value == null || value.isBlank()) return null;
        try { return SubscriptionPlan.valueOf(value); } catch (IllegalArgumentException e) { return null; }
    }
}
