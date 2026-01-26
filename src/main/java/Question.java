public class Question {
    private final int id; // Короткий ID для кнопок
    private final String text;
    private final boolean optional;
    private final String validationRegex;

    public Question(int id, String text, boolean optional, String validationRegex) {
        this.id = id;
        this.text = text;
        this.optional = optional;
        this.validationRegex = validationRegex;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public boolean isOptional() { return optional; }
    public String getValidationRegex() { return validationRegex; }
}