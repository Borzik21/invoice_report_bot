import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.*;

public class KeyboardFactory {
    public static InlineKeyboardMarkup createInlineKeyboard(Map<String, String> buttons) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        buttons.forEach((text, value) -> {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            if (value.startsWith("http")) {
                button.setUrl(value);
            } else {
                button.setCallbackData(value);
            }
            rows.add(Collections.singletonList(button));
        });

        markup.setKeyboard(rows);
        return markup;
    }
}