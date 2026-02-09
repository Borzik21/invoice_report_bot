import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserSession {
    private State state = State.IDLE;
    private List<Question> flowQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private final Map<String, String> answers = new LinkedHashMap<>();
    private String fieldToEdit;

    private long lastActivityTime = System.currentTimeMillis();

    public void refreshActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public List<Question> getFlowQuestions() { return flowQuestions; }
    public void setFlowQuestions(List<Question> flowQuestions) { this.flowQuestions = flowQuestions; }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }

    public Map<String, String> getAnswers() { return answers; }

    public String getFieldToEdit() { return fieldToEdit; }
    public void setFieldToEdit(String fieldToEdit) { this.fieldToEdit = fieldToEdit; }
}