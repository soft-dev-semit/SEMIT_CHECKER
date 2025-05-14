package csit.semit.semitchecker;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class AppLifecycleManager {

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public AppLifecycleManager() {
        // Запускаємо фоновий потік
        backgroundExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Ваш код
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        // Зупинити фоновий потік
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Спробувати завершити MySQL Cleanup Thread
        try {
            Class<?> cleanupClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            cleanupClass.getMethod("checkedShutdown").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // Немає MySQL — нічого страшного
        } catch (Exception e) {
            e.printStackTrace(); // Логування, якщо потрібно
        }
    }
}
