import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            ReportBot bot = new ReportBot();
            botsApi.registerBot(bot);
            System.out.println("Бот успешно запущен!");

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(bot::cleanUpSessions, 1, 1, TimeUnit.HOURS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}