import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.*;

public class KeyboardFactory {

    public static InlineKeyboardMarkup createInlineKeyboard(Map<String, String> buttons) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        buttons.forEach((text, value) -> {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            if (value.startsWith("http")) button.setUrl(value);
            else button.setCallbackData(value);
            rows.add(Collections.singletonList(button));
        });
        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup createGridKeyboard(List<String> options, int columns, String callbackPrefix) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (String option : options) {
            InlineKeyboardButton button = new InlineKeyboardButton(option);
            button.setCallbackData(callbackPrefix + option);
            row.add(button);

            if (row.size() == columns) {
                rows.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }
}