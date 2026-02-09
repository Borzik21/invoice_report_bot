import java.util.List;

public class Question {
    private final int id;
    private final String text;
    private final boolean optional;
    private final String validationRegex;
    private final List<String> options;

    public Question(int id, String text, boolean optional, String validationRegex) {
        this(id, text, optional, validationRegex, null);
    }

    public Question(int id, String text, boolean optional, String validationRegex, List<String> options) {
        this.id = id;
        this.text = text;
        this.optional = optional;
        this.validationRegex = validationRegex;
        this.options = options;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public boolean isOptional() { return optional; }
    public String getValidationRegex() { return validationRegex; }
    public List<String> getOptions() { return options; }
}