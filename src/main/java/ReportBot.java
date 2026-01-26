import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportBot extends TelegramLongPollingBot {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final UpdateHandler handler = new UpdateHandler(this, sessions);

    private final String REPORT_CHAT_ID = "-1002295722378";
    private final Set<Long> ADMIN_IDS = Set.of(7474534847L);

    @Override
    public String getBotUsername() { return "ВАШ_БОТ"; }
    @Override
    public String getBotToken() { return System.getenv("BOT_TOKEN"); }

    @Override
    public void onUpdateReceived(Update update) {
        // ВОЗВРАЩАЕМ ЛОГИ
        if (update.hasMessage()) {
            System.out.println("[ID: " + update.getMessage().getFrom().getId() + "] [@" + update.getMessage().getFrom().getUserName() + "] ТЕКСТ: " + update.getMessage().getText());
            handler.handleText(update);
        } else if (update.hasCallbackQuery()) {
            System.out.println("[ID: " + update.getCallbackQuery().getFrom().getId() + "] [@" + update.getCallbackQuery().getFrom().getUserName() + "] КНОПКА: " + update.getCallbackQuery().getData());
            handler.handleCallback(update);
        }
    }

    // --- МЕТОДЫ ОТПРАВКИ (Бот только шлет сообщения) ---

    public void editMenu(long chatId, int messageId, String newText, Map<String, String> buttons) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(newText);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(KeyboardFactory.createInlineKeyboard(buttons));
        try {
            execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startSurvey(long chatId, UserSession session) {
        session.getAnswers().clear();
        session.setCurrentQuestionIndex(0);
        session.setState(State.ASK_EXISTING);
        sendMenu(chatId, "Инвойс для существующего клиента?", Map.of("Да ✅", "exist_yes", "Нет ❌", "exist_no"));
    }

    public void askNext(long chatId, UserSession session) {
        Question q = session.getFlowQuestions().get(session.getCurrentQuestionIndex());
        if (q.isOptional()) {
            sendMenu(chatId, "📝 " + q.getText() + ":", Map.of("Пропустить ⏭", "skip_question"));
        } else {
            sendText(chatId, "📝 " + q.getText() + ":");
        }
    }

    public void showReview(long chatId, UserSession session, String username) {
        session.setState(State.REVIEW);
        StringBuilder sb = new StringBuilder("📋 *ПРОВЕРКА ДАННЫХ*\n@").append(username).append("\n\n");
        session.getAnswers().forEach((k, v) -> sb.append("*").append(k).append("*: ").append(v).append("\n"));
        sendMenu(chatId, sb.toString(), Map.of("Все верно ✅", "send_final", "Изменить ❌", "edit_all", "🔄 Сбросить", "restart_all"));
    }

    public void sendFinalReport(long chatId, int messageId, UserSession session, String senderUsername) {
        StringBuilder report = new StringBuilder("🚀 *НОВЫЙ ИНВОЙС*\n");
        report.append("👤 *Отправитель:* @").append(senderUsername).append("\n\n"); // Добавляем имя

        session.getAnswers().forEach((k, v) -> report.append("*").append(k).append("*: ").append(v).append("\n"));

        try {
            sendText(Long.parseLong(REPORT_CHAT_ID), report.toString());
        } catch (Exception e) {
            System.err.println("Ошибка отправки в группу: " + e.getMessage());
        }

        Map<String, String> finalButtons = new LinkedHashMap<>();
        finalButtons.put("🆕 Создать новый инвойс", "restart_all");
        finalButtons.put("🔮 Узнать свою судьбу 🔮", "https://t.me/Your1Prediction_Bot");

        String successText = "✅ *Отчет успешно отправлен!*\n\n";
        editMenu(chatId, messageId, successText, finalButtons);

        sessions.remove(chatId);
    }
    public boolean isUserAuthorized(long userId) {
        if (ADMIN_IDS.contains(userId)) return true;
        try {
            ChatMember cm = execute(new GetChatMember(REPORT_CHAT_ID, userId));
            return !cm.getStatus().equals("left") && !cm.getStatus().equals("kicked");
        } catch (Exception e) { return false; }
    }

    public void sendText(long chatId, String text) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    public void sendMenu(long chatId, String text, Map<String, String> buttons) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        sm.setReplyMarkup(KeyboardFactory.createInlineKeyboard(buttons));
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    public void removeButtons(long chatId, int messageId) {
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setReplyMarkup(null);
        try { execute(edit); } catch (Exception e) {}
    }
}