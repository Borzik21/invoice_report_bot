import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(ReportBot.class);
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final UpdateHandler handler = new UpdateHandler(this, sessions);

    private final String REPORT_CHAT_ID = System.getenv("REPORT_CHAT_ID");
    private final String BOT_NAME = System.getenv("BOT_NAME");
    private final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    private final Set<Long> ADMIN_IDS;

    public ReportBot() {
        if (BOT_TOKEN == null || BOT_TOKEN.isBlank()) {
            throw new RuntimeException("ОШИБКА: Переменная BOT_TOKEN не задана!");
        }
        if (REPORT_CHAT_ID == null || REPORT_CHAT_ID.isBlank()) {
            throw new RuntimeException("ОШИБКА: Переменная REPORT_CHAT_ID не задана!");
        }
        if (BOT_NAME == null || BOT_NAME.isBlank()) {
            throw new RuntimeException("ОШИБКА: Переменная BOT_NAME не задана!");
        }

        String adminsEnv = System.getenv("ADMIN_IDS");
        if (adminsEnv != null && !adminsEnv.isBlank()) {
            ADMIN_IDS = Arrays.stream(adminsEnv.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        } else {
            throw new RuntimeException("ОШИБКА: Переменная ADMIN_IDS не задана!");
        }
    }
    @Override
    public void clearWebhook() {    }
    @Override
    public String getBotUsername() { return BOT_NAME; }
    @Override
    public String getBotToken() { return BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = null;
        if (update.hasMessage()) chatId = update.getMessage().getChatId();
        else if (update.hasCallbackQuery()) chatId = update.getCallbackQuery().getMessage().getChatId();

        if (chatId != null && sessions.containsKey(chatId)) {
            sessions.get(chatId).refreshActivity();
        }

        if (update.hasMessage()) {
            logger.info("[ID: {}] [@{}] TEXT: {}",
                    update.getMessage().getFrom().getId(),
                    update.getMessage().getFrom().getUserName(),
                    update.getMessage().getText());
            handler.handleText(update);
        } else if (update.hasCallbackQuery()) {
            logger.info("[ID: {}] [@{}] BUTTON: {}",
                    update.getCallbackQuery().getFrom().getId(),
                    update.getCallbackQuery().getFrom().getUserName(),
                    update.getCallbackQuery().getData());
            handler.handleCallback(update);
        }
    }

    public void cleanUpSessions() {
        long timeout = 60 * 60 * 1000; // 1 час
        long now = System.currentTimeMillis();
        int before = sessions.size();
        sessions.entrySet().removeIf(entry -> (now - entry.getValue().getLastActivityTime()) > timeout);
        int after = sessions.size();
        if (before != after) {
            logger.info("Очистка сессий: удалено {} старых записей.", (before - after));
        }
    }

    public void editMenu(long chatId, int messageId, String newText, Map<String, String> buttons) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(newText);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.createInlineKeyboard(buttons));
        try { execute(edit); } catch (Exception e) { logger.error("Ошибка editMenu: ", e); }
    }

    public void startSurvey(long chatId, UserSession session) {
        session.getAnswers().clear();
        session.setCurrentQuestionIndex(0);
        session.setState(State.ASK_EXISTING);
        sendMenu(
                chatId,
                "Инвойс для существующего клиента?",
                Map.of("Да ✅", "exist_yes", "Нет ❌", "exist_no")
        );
    }

    public void askNext(long chatId, UserSession session) {
        Question q = session.getFlowQuestions().get(session.getCurrentQuestionIndex());

        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            SendMessage sm = new SendMessage(String.valueOf(chatId), "📝 " + q.getText() + " (выберите вариант):");
            sm.setParseMode("Markdown");
            sm.setReplyMarkup(KeyboardFactory.createGridKeyboard(q.getOptions(), 2, "opt_"));
            try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
        }
        else if (q.isOptional()) {
            sendMenu(chatId, "📝 " + q.getText() + ":", Map.of("Пропустить ⏭", "skip_question"));
        }
        else {
            sendText(chatId, "📝 " + q.getText() + ":");
        }
    }

    public void sendGridOption(long chatId, String text, List<String> options) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        sm.setReplyMarkup(KeyboardFactory.createGridKeyboard(options, 2, "opt_"));
        try { execute(sm); } catch (Exception e) { logger.error("Ошибка отправки кнопок редактирования: ", e); }
    }

    public void showReview(long chatId, UserSession session, String username) {
        session.setState(State.REVIEW);
        StringBuilder sb = new StringBuilder("📋 *ПРОВЕРКА ДАННЫХ*\n@").append(username).append("\n\n");
        session.getAnswers().forEach((k, v) -> sb.append("*").append(k).append("*: ").append(v).append("\n"));
        sendMenu(chatId, sb.toString(), Map.of("Все верно ✅", "send_final", "Изменить ❌", "edit_all", "🔄 Сбросить", "restart_all"));
    }

    public void sendFinalReport(long chatId, int messageId, UserSession session, String senderUsername) {
        StringBuilder report = new StringBuilder("🚀 *НОВЫЙ ИНВОЙС*\n");
        report.append("👤 *Отправитель:* @").append(senderUsername).append("\n\n");
        session.getAnswers().forEach((k, v) -> report.append("*").append(k).append("*: ").append(v).append("\n"));

        try {
            sendText(Long.parseLong(REPORT_CHAT_ID), report.toString());
        } catch (Exception e) {
            logger.error("Ошибка отправки репорта в группу: ", e);
            sendText(chatId, "⚠️ Ошибка при отправке отчета администратору.");
            return;
        }

        Map<String, String> finalButtons = new LinkedHashMap<>();
        finalButtons.put("🆕 Создать новый инвойс 🆕", "restart_all");
        finalButtons.put("🔮 Узнать свою судьбу 🔮", "https://t.me/Your1Prediction_Bot");
        finalButtons.put("\uD83D\uDCAA FITNESS мотивация подъехала! \uD83C\uDFCB\uFE0F", "https://t.me/fitmotivation_bot");

        String successText = "✅ *Отчет успешно отправлен!*\n\n";
        editMenu(chatId, messageId, successText, finalButtons);
        sessions.remove(chatId);
    }

    public boolean isUserAuthorized(long userId) {
        if (ADMIN_IDS.contains(userId)) return true;
        try {
            ChatMember cm = execute(new GetChatMember(REPORT_CHAT_ID, userId));
            return !cm.getStatus().equals("left") && !cm.getStatus().equals("kicked");
        } catch (Exception e) {
            logger.warn("Ошибка проверки прав пользователя {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public void sendText(long chatId, String text) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { logger.error("Ошибка sendText: ", e); }
    }

    public void sendMenu(long chatId, String text, Map<String, String> buttons) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        sm.setReplyMarkup(KeyboardFactory.createInlineKeyboard(buttons));
        try { execute(sm); } catch (Exception e) { logger.error("Ошибка sendMenu: ", e); }
    }

    public void removeButtons(long chatId, int messageId) {
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setReplyMarkup(null);
        try { execute(edit); } catch (Exception e) { /* игнорируем */ }
    }
}