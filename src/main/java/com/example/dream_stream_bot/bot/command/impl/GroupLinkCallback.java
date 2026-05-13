package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardService;
import com.example.dream_stream_bot.service.subscription.GroupOwnerInviteService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class GroupLinkCallback implements CallbackHandler {

    private final GroupLinkWizardService groupLinkWizardService;
    private final GroupOwnerInviteService groupOwnerInviteService;

    public GroupLinkCallback(GroupLinkWizardService groupLinkWizardService,
                             GroupOwnerInviteService groupOwnerInviteService) {
        this.groupLinkWizardService = groupLinkWizardService;
        this.groupOwnerInviteService = groupOwnerInviteService;
    }

    @Override
    public String prefix() {
        return BotNavigationService.CALLBACK_GRP;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getChatId() == null || ctx.getBotEntity() == null || ctx.getUser() == null) {
            return CallbackHandler.silent();
        }
        if (!(ctx.getCallbackQuery().getMessage() instanceof Message msg)) {
            return CallbackHandler.silent();
        }
        ChatScope scope = ChatScope.fromMessageType(
                msg.isUserMessage(), msg.isGroupMessage(), msg.isSuperGroupMessage(), msg.isChannelMessage());
        if (scope != ChatScope.PRIVATE) {
            return CallbackHandler.silent();
        }
        Long chatId = ctx.getChatId();
        BotEntity bot = ctx.getBotEntity();
        UserEntity user = ctx.getUser();
        String payload = ctx.getPayload() == null ? "" : ctx.getPayload().trim();
        if (payload.isEmpty()) {
            return CallbackHandler.silent();
        }
        if ("begin".equals(payload)) {
            return groupLinkWizardService.openTariffPicker(chatId, bot);
        }
        if ("cancel".equals(payload)) {
            return groupLinkWizardService.onCancel(chatId, bot, user);
        }
        if ("go".equals(payload)) {
            return groupLinkWizardService.onConfirmGo(chatId, bot, user);
        }
        if ("retry".equals(payload)) {
            return groupLinkWizardService.onConfirmRetry(chatId, bot, user);
        }
        if (payload.startsWith("pick:")) {
            long tariffId;
            try {
                tariffId = Long.parseLong(payload.substring("pick:".length()).trim());
            } catch (NumberFormatException e) {
                return CallbackHandler.silent();
            }
            return groupLinkWizardService.onPickTariff(chatId, bot, user, tariffId);
        }
        if (payload.startsWith("pay:detail:")) {
            long subscriptionId;
            try {
                subscriptionId = Long.parseLong(payload.substring("pay:detail:".length()).trim());
            } catch (NumberFormatException e) {
                return CallbackHandler.silent();
            }
            return groupLinkWizardService.groupPayDetailPreview(chatId, bot, user, subscriptionId);
        }
        if (payload.startsWith("pay:open:")) {
            long subscriptionId;
            try {
                subscriptionId = Long.parseLong(payload.substring("pay:open:".length()).trim());
            } catch (NumberFormatException e) {
                return CallbackHandler.silent();
            }
            return groupLinkWizardService.groupPayOpenCheckout(chatId, bot, user, subscriptionId);
        }
        if (payload.startsWith("invite:")) {
            long subscriptionId;
            try {
                subscriptionId = Long.parseLong(payload.substring("invite:".length()).trim());
            } catch (NumberFormatException e) {
                return CallbackHandler.silent();
            }
            // Отправка в группу выполняется здесь (trySend), а не через ответы диспетчера — иначе нет признака ошибки Telegram API.
            return groupOwnerInviteService.sendInviteToGroupAndNotifyOwner(
                    chatId, bot, user, subscriptionId, ctx.getSender());
        }
        return CallbackHandler.silent();
    }
}
