import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.*;

public class UpdateHandler {
    private final ReportBot bot;
    private final Map<Long, UserSession> sessions;

    public UpdateHandler(ReportBot bot, Map<Long, UserSession> sessions) {
        this.bot = bot;
        this.sessions = sessions;
    }

    public void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        UserSession session = sessions.computeIfAbsent(chatId, k -> new UserSession());

        if (text.equals("/start")) {
            if (bot.isUserAuthorized(update.getMessage().getFrom().getId())) {
                bot.startSurvey(chatId, session);
            } else {
                bot.sendText(chatId, "Доступ запрещен.");
            }
            return;
        }

        if (session.getState() == State.FILLING_DATA || session.getState() == State.EDITING_FIELD) {
            processInput(chatId, text, session, update.getMessage().getFrom().getUserName());
        }
    }

    public void handleCallback(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        UserSession session = sessions.get(chatId);

        if (session == null && data.equals("restart_all")) {
            session = new UserSession();
            sessions.put(chatId, session);
        } else if (session == null) {
            return;
        }

        bot.removeButtons(chatId, update.getCallbackQuery().getMessage().getMessageId());

        if (data.equals("restart_all")) {
            bot.startSurvey(chatId, session);
        }  else if (data.startsWith("exist_")) {
            session.getAnswers().put("Клиент", data.equals("exist_yes") ? "Существующий" : "Новый");
            session.setState(State.ASK_TYPE);
            bot.sendMenu(chatId, "Инвойс на физлицо или компанию?", Map.of("Физлицо👤", "type_person", "Компания🏢", "type_company"));
        } else if (data.startsWith("type_")) {
            session.getAnswers().put("Тип", data.equals("type_person") ? "Физлицо" : "Компания");
            session.setFlowQuestions(QuestionFlowManager.getFlow(session.getAnswers().get("Клиент"), session.getAnswers().get("Тип")));
            session.setState(State.FILLING_DATA);
            session.setCurrentQuestionIndex(0);
            bot.askNext(chatId, session);
        } else if (data.equals("skip_question")) {
            Question q = session.getFlowQuestions().get(session.getCurrentQuestionIndex());
            session.getAnswers().put(q.getText(), "—");
            moveToNext(chatId, session, update.getCallbackQuery().getFrom().getUserName());
        } else if (data.equals("edit_all")) {
            Map<String, String> fields = new LinkedHashMap<>();
            session.getFlowQuestions().forEach(q -> {
                fields.put(q.getText(), "edit_id_" + q.getId()); // Используем ID!
            });
            fields.put("🔄 Начать заново", "restart_all");
            bot.sendMenu(chatId, "Что изменить?", fields);
        } else if (data.startsWith("edit_id_")) {
            int qId = Integer.parseInt(data.replace("edit_id_", ""));
            Question q = session.getFlowQuestions().stream().filter(que -> que.getId() == qId).findFirst().orElse(null);
            if (q != null) {
                session.setFieldToEdit(q.getText());
                session.setState(State.EDITING_FIELD);
                bot.sendText(chatId, "Введите новое значение для *" + q.getText() + "*:");
            }
        } else if (data.equals("send_final")) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            String username = update.getCallbackQuery().getFrom().getUserName();
            if (username == null) {
                username = update.getCallbackQuery().getFrom().getFirstName();
            }

            bot.sendFinalReport(chatId, messageId, session, username);
        }
    }

    private void processInput(long chatId, String text, UserSession session, String username) {
        Question currentQ = (session.getState() == State.EDITING_FIELD)
                ? session.getFlowQuestions().stream().filter(q -> q.getText().equals(session.getFieldToEdit())).findFirst().orElse(null)
                : session.getFlowQuestions().get(session.getCurrentQuestionIndex());

        if (currentQ != null && currentQ.getValidationRegex() != null && !text.matches(currentQ.getValidationRegex())) {
            bot.sendText(chatId, "❌ Ошибка формата! Ожидается: " + currentQ.getText());
            return;
        }

        session.getAnswers().put(currentQ.getText(), text);

        if (session.getState() == State.EDITING_FIELD) {
            bot.showReview(chatId, session, username);
        } else {
            moveToNext(chatId, session, username);
        }
    }

    private void moveToNext(long chatId, UserSession session, String username) {
        if (session.getCurrentQuestionIndex() + 1 < session.getFlowQuestions().size()) {
            session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
            bot.askNext(chatId, session);
        } else {
            bot.showReview(chatId, session, username);
        }
    }
}