package novayoung.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 *
 * Created by pengchangguo on 17/1/5.
 */
@SpringBootApplication
public class LookLogTestApplication {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext applicationContext = SpringApplication.run(LookLogTestApplication.class, args);
        LookLogAppender.setApplicationContext(applicationContext);

        LogService logService = applicationContext.getBean(LogService.class);

        while (true) {
            logService.log();
            Thread.sleep(3000);
        }

    }
}
