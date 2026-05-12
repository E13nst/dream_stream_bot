package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.agent.AgentProvider;
import com.example.dream_stream_bot.model.agent.AgentRole;
import com.example.dream_stream_bot.model.agent.DataLocality;
import com.example.dream_stream_bot.model.consent.ConsentCode;
import com.example.dream_stream_bot.model.consent.ConsentDocumentEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotType;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.admin.AdminUserDetailsService;
import com.example.dream_stream_bot.service.agent.AgentConfigService;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.settings.SystemSettingsService;
import com.example.dream_stream_bot.service.access.GatingDedup;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.comparing;

@Controller
public class AdminWebController {

    private final UserService userService;
    private final BotService botService;
    private final AgentConfigService agentConfigService;
    private final ConsentService consentService;
    private final AdminUserDetailsService adminUserDetailsService;
    private final SystemSettingsService systemSettingsService;

    public AdminWebController(UserService userService, BotService botService,
                              AgentConfigService agentConfigService,
                              ConsentService consentService,
                              AdminUserDetailsService adminUserDetailsService,
                              SystemSettingsService systemSettingsService) {
        this.userService = userService;
        this.botService = botService;
        this.agentConfigService = agentConfigService;
        this.consentService = consentService;
        this.adminUserDetailsService = adminUserDetailsService;
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        long usersCount = userService.findAll().size();
        long adminsCount = userService.findByRole(UserEntity.UserRole.ADMIN).size();
        long botsCount = botService.findAll().size();
        long activeBotsCount = botService.findActiveBots().size();

        model.addAttribute("usersCount", usersCount);
        model.addAttribute("adminsCount", adminsCount);
        model.addAttribute("botsCount", botsCount);
        model.addAttribute("activeBotsCount", activeBotsCount);
        model.addAttribute("showPasswordWarning", adminUserDetailsService.isDefaultPassword());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String usersPage(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("users", filterUsers(query));
        return "admin/users";
    }

    @GetMapping("/admin/users/table")
    public String usersTable(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("users", filterUsers(query));
        return "admin/fragments/users-table :: usersTable";
    }

    @GetMapping("/admin/bots")
    public String botsPage(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        List<BotEntity> bots = getSortedBots();
        Long normalizedId = normalizeSelectedId(selectedId, bots);
        if (selectedId != null && normalizedId == null) {
            return "redirect:/admin/bots";
        }
        fillBotsModel(model, normalizedId);
        return "admin/bots";
    }

    @GetMapping("/admin/bots/table")
    public String botsTable(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        fillBotsModel(model, normalizeSelectedId(selectedId, getSortedBots()));
        return "admin/fragments/bots-table :: botsTable";
    }

    @GetMapping("/admin/bots/details")
    public String botDetails(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        fillBotsModel(model, normalizeSelectedId(selectedId, getSortedBots()));
        return "admin/fragments/bot-details :: botDetails";
    }

    @PostMapping("/admin/bots/create")
    public String createBot(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String token,
            @RequestParam String type,
            @RequestParam(name = "requireAgeConfirmation", defaultValue = "false") boolean requireAgeConfirmation,
            @RequestParam(required = false) Long agentConfigId,
            @RequestParam(required = false) String webhookUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String miniapp,
            @RequestParam(required = false) String keywords,
            @RequestParam(name = "isActive", defaultValue = "false") boolean isActive,
            @RequestParam(required = false) String yookassaShopId,
            @RequestParam(required = false) String yookassaSecretKey,
            @RequestParam(name = "yookassaReceiptEnabled", defaultValue = "false") boolean yookassaReceiptEnabled) {
        String typeValue = BotType.fromString(type).getValue();
        if (BotType.ASSISTANT.getValue().equalsIgnoreCase(typeValue) && agentConfigId == null) {
            return "redirect:/admin/bots?error=assistantNeedsAgent";
        }
        BotEntity bot = new BotEntity();
        bot.setName(name.trim());
        bot.setUsername(username.trim());
        bot.setToken(token.trim());
        bot.setType(typeValue);
        bot.setWebhookUrl(blankToNull(webhookUrl));
        bot.setDescription(blankToNull(description));
        bot.setMiniapp(blankToNull(miniapp));
        bot.setIsActive(isActive);
        bot.setRequireAgeConfirmation(requireAgeConfirmation);
        bot.setYookassaShopId(blankToNull(yookassaShopId));
        if (yookassaSecretKey != null && !yookassaSecretKey.isBlank()) {
            bot.setYookassaSecretKey(yookassaSecretKey.trim());
        }
        bot.setYookassaReceiptEnabled(yookassaReceiptEnabled);
        if (agentConfigId != null) {
            bot.setAgentConfig(agentConfigService.requireById(agentConfigId));
        }
        BotEntity saved = botService.save(bot);
        botService.replaceKeywords(saved.getId(), splitKeywords(keywords));
        return "redirect:/admin/bots?selectedId=" + saved.getId();
    }

    @PostMapping("/admin/bots/{id}/update")
    public String updateBot(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String type,
            @RequestParam(name = "requireAgeConfirmation", defaultValue = "false") boolean requireAgeConfirmation,
            @RequestParam(required = false) Long agentConfigId,
            @RequestParam(required = false) String webhookUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String miniapp,
            @RequestParam(required = false) String keywords,
            @RequestParam(name = "isActive", defaultValue = "false") boolean isActive,
            @RequestParam(required = false) String yookassaShopId,
            @RequestParam(required = false) String yookassaSecretKey,
            @RequestParam(name = "yookassaReceiptEnabled", defaultValue = "false") boolean yookassaReceiptEnabled) {
        BotEntity bot = botService.findById(id);
        if (bot != null) {
            String typeValue = BotType.fromString(type).getValue();
            bot.setName(name.trim());
            bot.setUsername(username.trim());
            bot.setType(typeValue);
            bot.setWebhookUrl(blankToNull(webhookUrl));
            bot.setDescription(blankToNull(description));
            bot.setMiniapp(blankToNull(miniapp));
            bot.setIsActive(isActive);
            bot.setRequireAgeConfirmation(requireAgeConfirmation);
            bot.setYookassaShopId(blankToNull(yookassaShopId));
            if (yookassaSecretKey != null && !yookassaSecretKey.isBlank()) {
                bot.setYookassaSecretKey(yookassaSecretKey.trim());
            }
            bot.setYookassaReceiptEnabled(yookassaReceiptEnabled);

            if (BotType.COPYCAT.getValue().equalsIgnoreCase(typeValue)) {
                bot.setAgentConfig(null);
            } else if (BotType.ASSISTANT.getValue().equalsIgnoreCase(typeValue)) {
                if (agentConfigId != null) {
                    bot.setAgentConfig(agentConfigService.requireById(agentConfigId));
                }
            }

            botService.save(bot);
            botService.replaceKeywords(id, splitKeywords(keywords));
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    @PostMapping("/admin/bots/{id}/consents")
    public String updateBotConsents(@PathVariable Long id,
                                    @RequestParam Map<String, String> params,
                                    RedirectAttributes redirectAttributes) {
        BotEntity bot = botService.findById(id);
        if (bot == null) {
            redirectAttributes.addFlashAttribute("error", "Бот не найден");
            return "redirect:/admin/bots";
        }
        try {
            for (ConsentCode code : ConsentCode.values()) {
                String raw = params.get("binding_" + code.name());
                if (raw == null || raw.isBlank()) {
                    consentService.clearBindingForBot(id, code);
                    continue;
                }
                Long documentId = Long.parseLong(raw);
                consentService.bindDocumentToBot(id, code, documentId);
            }
            redirectAttributes.addFlashAttribute("success", "Привязки документов сохранены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    @GetMapping("/admin/agents")
    public String agentsPage(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        List<AgentConfigEntity> agents = agentConfigService.findAll().stream()
                .sorted(comparing(AgentConfigEntity::getId))
                .toList();
        Long normalizedId = normalizeSelectedAgentId(selectedId, agents);
        if (selectedId != null && normalizedId == null) {
            return "redirect:/admin/agents";
        }
        fillAgentsModel(model, normalizedId, agents);
        return "admin/agents";
    }

    @PostMapping("/admin/agents/new")
    public String createAgent(
            @RequestParam String name,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String shortDescription,
            @RequestParam(defaultValue = "CONVERSATION") String role,
            @RequestParam(defaultValue = "OPENAI") String provider,
            @RequestParam(defaultValue = "CROSS_BORDER") String dataLocality,
            @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
            @RequestParam(defaultValue = "gpt-4o") String model,
            @RequestParam(required = false) Double temperature,
            @RequestParam(required = false) Double topP,
            @RequestParam(required = false) Double frequencyPenalty,
            @RequestParam(required = false) Double presencePenalty,
            @RequestParam(required = false) String systemPrompt,
            @RequestParam(required = false) Integer memWindow) {
        AgentConfigEntity e = new AgentConfigEntity();
        e.setName(name.trim());
        e.setDisplayName(blankToNull(displayName));
        e.setShortDescription(blankToNull(shortDescription));
        e.setRole(AgentRole.valueOf(role));
        e.setProvider(AgentProvider.valueOf(provider));
        e.setDataLocality(DataLocality.valueOf(dataLocality));
        e.setPublic(isPublic);
        e.setModel(model.trim());
        e.setTemperature(temperature);
        e.setTopP(topP);
        e.setFrequencyPenalty(frequencyPenalty);
        e.setPresencePenalty(presencePenalty);
        e.setSystemPrompt(blankToNull(systemPrompt));
        e.setMemWindow(memWindow != null ? memWindow : 100);
        AgentConfigEntity saved = agentConfigService.save(e);
        return "redirect:/admin/agents?selectedId=" + saved.getId();
    }

    @PostMapping("/admin/agents/{id}/update")
    public String updateAgent(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String shortDescription,
            @RequestParam String role,
            @RequestParam String provider,
            @RequestParam(defaultValue = "CROSS_BORDER") String dataLocality,
            @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
            @RequestParam String model,
            @RequestParam(required = false) Double temperature,
            @RequestParam(required = false) Double topP,
            @RequestParam(required = false) Double frequencyPenalty,
            @RequestParam(required = false) Double presencePenalty,
            @RequestParam(required = false) String systemPrompt,
            @RequestParam(required = false) Integer memWindow) {
        agentConfigService.update(
                id,
                name.trim(),
                blankToNull(displayName),
                blankToNull(shortDescription),
                AgentRole.valueOf(role),
                AgentProvider.valueOf(provider),
                DataLocality.valueOf(dataLocality),
                isPublic,
                model.trim(),
                temperature,
                topP,
                frequencyPenalty,
                presencePenalty,
                blankToNull(systemPrompt),
                memWindow);
        return "redirect:/admin/agents?selectedId=" + id;
    }

    private Long normalizeSelectedAgentId(Long selectedId, List<AgentConfigEntity> agents) {
        if (selectedId == null) {
            return null;
        }
        return agents.stream().anyMatch(a -> a.getId().equals(selectedId)) ? selectedId : null;
    }

    private void fillAgentsModel(Model model, Long selectedId, List<AgentConfigEntity> agents) {
        Map<Long, Long> agentUsageCounts = new LinkedHashMap<>();
        for (AgentConfigEntity a : agents) {
            agentUsageCounts.put(a.getId(), botService.countBotsUsingAgent(a.getId()));
        }
        model.addAttribute("agentUsageCounts", agentUsageCounts);
        model.addAttribute("agents", agents);
        model.addAttribute("selectedId", selectedId);
        model.addAttribute("roles", AgentRole.values());
        model.addAttribute("providers", AgentProvider.values());
        model.addAttribute("dataLocalities", DataLocality.values());

        Optional<AgentConfigEntity> selected = selectedId == null
                ? Optional.empty()
                : agents.stream().filter(a -> a.getId().equals(selectedId)).findFirst();
        model.addAttribute("selectedAgent", selected.orElse(null));
        if (selectedId != null && selected.isPresent()) {
            model.addAttribute("selectedAgentBotCount", botService.countBotsUsingAgent(selectedId));
        } else {
            model.addAttribute("selectedAgentBotCount", 0L);
        }
        model.addAttribute("openAgentDetailsModal", selectedId != null && selected.isPresent());
    }

    @PostMapping("/admin/bots/{id}/delete")
    public String deleteBot(@PathVariable Long id) {
        if (botService.existsById(id)) {
            botService.deleteById(id);
        }
        return "redirect:/admin/bots";
    }

    @PostMapping("/admin/bots/{id}/toggle")
    public String toggleBotStatus(@PathVariable Long id) {
        BotEntity bot = botService.findById(id);
        if (bot != null) {
            bot.setIsActive(!Boolean.TRUE.equals(bot.getIsActive()));
            botService.save(bot);
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    /**
     * {@code null} если параметр {@code null}; иначе id только если бот с таким id есть в списке.
     */
    private Long normalizeSelectedId(Long selectedId, List<BotEntity> bots) {
        if (selectedId == null) {
            return null;
        }
        return bots.stream().anyMatch(b -> b.getId().equals(selectedId)) ? selectedId : null;
    }

    private void fillBotsModel(Model model, Long selectedId) {
        List<BotEntity> bots = getSortedBots();
        model.addAttribute("bots", bots);
        model.addAttribute("botTypes", BotType.values());
        model.addAttribute("selectedId", selectedId);

        List<AgentConfigEntity> allAgents = agentConfigService.findAll().stream()
                .sorted(comparing(AgentConfigEntity::getId))
                .toList();
        model.addAttribute("allAgents", allAgents);

        Optional<BotEntity> selectedBot = selectedId == null
                ? Optional.empty()
                : Optional.ofNullable(botService.findById(selectedId));

        model.addAttribute("selectedBot", selectedBot.orElse(null));
        model.addAttribute("selectedBotKeywords", selectedBot
                .map(bot -> String.join(", ", bot.getBotTriggersList()))
                .orElse(""));
        // String keys: Thymeleaf map[enum] lookup is unreliable; template uses .get(code.name()).
        Map<String, List<ConsentDocumentEntity>> documentsByCode = new LinkedHashMap<>();
        for (ConsentCode code : ConsentCode.values()) {
            documentsByCode.put(code.name(), consentService.listVersions(code));
        }
        model.addAttribute("consentCodes", ConsentCode.values());
        model.addAttribute("consentDocumentsByCode", documentsByCode);
        model.addAttribute("selectedBotConsentBindings", selectedBot
                .map(bot -> consentService.activeDocumentsByBot(bot.getId()))
                .orElseGet(Map::of));
        model.addAttribute("openBotDetailsModal", selectedId != null && selectedBot.isPresent());
    }

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @GetMapping("/admin/settings")
    public String settingsPage(Model model) {
        model.addAttribute("retentionDays", systemSettingsService.getRetentionDaysAfterExpiry());
        model.addAttribute("retentionUnlimited", systemSettingsService.isRetentionUnlimited());
        model.addAttribute("gatingDedupHours",
                systemSettingsService.getInt(GatingDedup.KEY_GATING_DEDUP_HOURS, 24));
        return "admin/settings";
    }

    @PostMapping("/admin/settings/system")
    public String saveSystemSettings(
            @RequestParam int retentionDaysAfterExpiry,
            @RequestParam(name = "retentionUnlimited", defaultValue = "false") boolean retentionUnlimited,
            @RequestParam int gatingDedupHours,
            RedirectAttributes redirectAttributes) {
        if (retentionDaysAfterExpiry >= 1 && retentionDaysAfterExpiry <= 3650) {
            systemSettingsService.set(SystemSettingsService.KEY_RETENTION_DAYS_AFTER_EXPIRY,
                    String.valueOf(retentionDaysAfterExpiry));
        }
        systemSettingsService.set(SystemSettingsService.KEY_RETENTION_UNLIMITED, Boolean.toString(retentionUnlimited));
        if (gatingDedupHours >= 1 && gatingDedupHours <= 168) {
            systemSettingsService.set(GatingDedup.KEY_GATING_DEDUP_HOURS, String.valueOf(gatingDedupHours));
        }
        redirectAttributes.addFlashAttribute("success", "Системные параметры сохранены");
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (newPassword == null || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль должен содержать не менее 8 символов");
            return "redirect:/admin/settings";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают");
            return "redirect:/admin/settings";
        }
        try {
            adminUserDetailsService.changePassword(currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменён");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    private List<UserEntity> filterUsers(String query) {
        List<UserEntity> users = userService.findAll();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return users.stream()
                .filter(user -> normalized.isEmpty()
                        || contains(user.getUsername(), normalized)
                        || contains(user.getFirstName(), normalized)
                        || contains(user.getLastName(), normalized)
                        || String.valueOf(user.getTelegramId()).contains(normalized))
                .sorted(Comparator.comparing(UserEntity::getId))
                .toList();
    }

    private List<BotEntity> getSortedBots() {
        return botService.findAll().stream()
                .sorted(Comparator.comparing(BotEntity::getId))
                .toList();
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
