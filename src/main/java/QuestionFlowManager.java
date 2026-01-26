import java.util.ArrayList;
import java.util.List;

public class QuestionFlowManager {
    public static List<Question> getFlow(String clientType, String entityType) {
        List<Question> flow = new ArrayList<>();
        boolean isExist = "Существующий".equals(clientType);
        boolean isPerson = "Физлицо".equals(entityType);

        int id = 1;
        if (isExist && isPerson) {
            flow.add(new Question(id++, "ФИО клиента", false, null));
        } else if (isExist && !isPerson) {
            flow.add(new Question(id++, "Название компании", false, null));
        } else {
            flow.add(new Question(id++, "ФИО клиента", false, null));
            flow.add(new Question(id++, "Телефон клиента", false, "\\+?\\d+"));
            flow.add(new Question(id++, "Email клиента", false, ".+@.+"));
            flow.add(new Question(id++, "Язык клиента", false, null));
            flow.add(new Question(id++, "Страна клиента", false, null));
            flow.add(new Question(id++, "Описание деятельности клиента", false, null));
            flow.add(new Question(id++, "Источник", false, null));
            flow.add(new Question(id++, "Кто агент (если есть)", true, null));

            if (!isPerson) {
                flow.add(new Question(id++, "Название компании", false, null));
                flow.add(new Question(id++, "Юрисдикция компании", false, null));
            }
        }

        flow.add(new Question(id++, "Наименование услуги", false, null));
        flow.add(new Question(id++, "Сумма", false, null));
        flow.add(new Question(id++, "Валюта цены (по прайсу)", false, null));
        flow.add(new Question(id++, "В какой валюте оплата", false, null));
        flow.add(new Question(id++, "Примечания по оплате", false, null));

        if (!isPerson) {
            flow.add(new Question(id++, "Название и реквизиты плательщика", true, null));
        }
        flow.add(new Question(id++, "На чей номер инвойс", false, null));

        return flow;
    }
}