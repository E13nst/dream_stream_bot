package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffAccessMode;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import com.example.dream_stream_bot.service.telegram.BotService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

@Controller
public class AdminTariffController {

    private final SubscriptionTariffService tariffService;
    private final BotService botService;

    public AdminTariffController(SubscriptionTariffService tariffService, BotService botService) {
        this.tariffService = tariffService;
        this.botService = botService;
    }

    @GetMapping("/admin/tariffs")
    public String list(@RequestParam(name = "botId", required = false) Long botId, Model model) {
        model.addAttribute("bots", botService.findAll().stream()
                .sorted(Comparator.comparing(com.example.dream_stream_bot.model.telegram.BotEntity::getId))
                .toList());
        if (botId == null) {
            model.addAttribute("tariffs", botService.findAll().stream().flatMap(b ->
                    tariffService.listByBot(b.getId()).stream()).sorted(Comparator
                    .comparing(SubscriptionTariffEntity::getBotId).thenComparingInt(SubscriptionTariffEntity::getSortOrder)).toList());
        } else {
            model.addAttribute("tariffs", tariffService.listByBot(botId));
        }
        model.addAttribute("filterBotId", botId);
        model.addAttribute("scopes", TariffScope.values());
        model.addAttribute("modes", TariffAccessMode.values());
        return "admin/tariffs";
    }

    @GetMapping("/admin/tariffs/new")
    public String newForm(@RequestParam Long botId, Model model, RedirectAttributes ra) {
        if (botService.findById(botId) == null) {
            ra.addFlashAttribute("error", "Бот не найден");
            return "redirect:/admin/tariffs";
        }
        model.addAttribute("bots", botService.findAll().stream()
                .sorted(Comparator.comparing(com.example.dream_stream_bot.model.telegram.BotEntity::getId)).toList());
        model.addAttribute("scopes", TariffScope.values());
        model.addAttribute("modes", TariffAccessMode.values());
        model.addAttribute("presetBotId", botId);
        SubscriptionTariffEntity draft = new SubscriptionTariffEntity();
        draft.setBotId(botId);
        draft.setActive(true);
        model.addAttribute("tariff", draft);
        model.addAttribute("isNew", true);
        return "admin/tariff-form";
    }

    @GetMapping("/admin/tariffs/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam Long botId,
                           Model model,
                           RedirectAttributes ra) {
        try {
            SubscriptionTariffEntity tariff = tariffService.requireForBot(botId, id);
            model.addAttribute("bots", botService.findAll().stream()
                    .sorted(Comparator.comparing(com.example.dream_stream_bot.model.telegram.BotEntity::getId)).toList());
            model.addAttribute("scopes", TariffScope.values());
            model.addAttribute("modes", TariffAccessMode.values());
            model.addAttribute("tariff", tariff);
            model.addAttribute("isNew", false);
            return "admin/tariff-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tariffs";
        }
    }

    @PostMapping("/admin/tariffs/create")
    public String create(@RequestParam Long botId,
                         @RequestParam String code,
                         @RequestParam String title,
                         @RequestParam TariffScope scope,
                         @RequestParam TariffAccessMode accessMode,
                         @RequestParam(required = false) Integer trialDays,
                         @RequestParam(required = false) Integer maxParticipants,
                         @RequestParam(defaultValue = "99") int sortOrder,
                         @RequestParam(required = false) Boolean active,
                         @RequestParam(required = false) Boolean defaultPersonal,
                         @RequestParam(required = false) Boolean defaultGroup,
                         @RequestParam(required = false) Boolean referralEnabled,
                         @RequestParam(required = false) Integer referralReferrerDays,
                         @RequestParam(required = false) Integer referralReferredDays,
                         @RequestParam(required = false) Boolean referralFirstPaymentOnly,
                         @RequestParam(required = false) String priceRubles,
                         @RequestParam(required = false) String currency,
                         @RequestParam(required = false) Integer paidTermDays,
                         @RequestParam(required = false) String checkoutDescription,
                         RedirectAttributes ra) {
        try {
            boolean activeEffective = !Boolean.FALSE.equals(active);
            boolean defP = Boolean.TRUE.equals(defaultPersonal);
            boolean defG = Boolean.TRUE.equals(defaultGroup);
            boolean refEnabled = Boolean.TRUE.equals(referralEnabled);
            boolean firstPaymentOnly = !Boolean.FALSE.equals(referralFirstPaymentOnly);
            Long priceMinor = parsePriceMinor(priceRubles);
            SubscriptionTariffEntity t = tariffService.create(botId, code.trim(), title, scope,
                    accessMode, trialDays, maxParticipants, sortOrder, activeEffective, defP, defG,
                    refEnabled, referralReferrerDays, referralReferredDays, firstPaymentOnly,
                    priceMinor, blankToNull(currency), paidTermDays, checkoutDescription);
            ra.addFlashAttribute("success", "Тариф «" + t.getCode() + "» создан");
            return "redirect:/admin/tariffs?botId=" + botId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tariffs/new?botId=" + botId;
        }
    }

    @PostMapping("/admin/tariffs/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam Long botId,
                         @RequestParam String code,
                         @RequestParam String title,
                         @RequestParam TariffScope scope,
                         @RequestParam TariffAccessMode accessMode,
                         @RequestParam(required = false) Integer trialDays,
                         @RequestParam(required = false) Integer maxParticipants,
                         @RequestParam(defaultValue = "99") int sortOrder,
                         @RequestParam(required = false) Boolean active,
                         @RequestParam(required = false) Boolean defaultPersonal,
                         @RequestParam(required = false) Boolean defaultGroup,
                         @RequestParam(required = false) Boolean referralEnabled,
                         @RequestParam(required = false) Integer referralReferrerDays,
                         @RequestParam(required = false) Integer referralReferredDays,
                         @RequestParam(required = false) Boolean referralFirstPaymentOnly,
                         @RequestParam(required = false) String priceRubles,
                         @RequestParam(required = false) String currency,
                         @RequestParam(required = false) Integer paidTermDays,
                         @RequestParam(required = false) String checkoutDescription,
                         RedirectAttributes ra) {
        try {
            boolean activeEffective = Boolean.TRUE.equals(active);
            boolean defP = Boolean.TRUE.equals(defaultPersonal);
            boolean defG = Boolean.TRUE.equals(defaultGroup);
            boolean refEnabled = Boolean.TRUE.equals(referralEnabled);
            boolean firstPaymentOnly = !Boolean.FALSE.equals(referralFirstPaymentOnly);
            Long priceMinor = parsePriceMinor(priceRubles);
            tariffService.update(id, botId, code.trim(), title, scope,
                    accessMode, trialDays, maxParticipants, sortOrder, activeEffective, defP, defG,
                    refEnabled, referralReferrerDays, referralReferredDays, firstPaymentOnly,
                    priceMinor, blankToNull(currency), paidTermDays, checkoutDescription);
            ra.addFlashAttribute("success", "Тариф обновлён");
            return "redirect:/admin/tariffs?botId=" + botId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tariffs/" + id + "/edit?botId=" + botId;
        }
    }

    @PostMapping("/admin/tariffs/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam Long botId, RedirectAttributes ra) {
        try {
            tariffService.delete(id, botId);
            ra.addFlashAttribute("success", "Тариф удалён");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error",
                    "Нельзя удалить тариф — есть активные подписки или записи триала. Снимите активность тарифа вместо удаления.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tariffs?botId=" + botId;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /**
     * Цена в рублях с не более чем 2 знаками после запятой; {@code null} если поле пустое.
     */
    private static Long parsePriceMinor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim()
                .replace('\u00A0', ' ')
                .replace(" ", "")
                .replace(',', '.');
        BigDecimal v = new BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY);
        if (v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительной");
        }
        try {
            return v.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("В цене допускается не более двух знаков после запятой");
        }
    }
}
