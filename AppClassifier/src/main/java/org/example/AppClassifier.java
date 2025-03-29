package org.example;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
public class AppClassifier
{
    private final TaskExecutor exec = new SimpleAsyncTaskExecutor();

    public static void main( String[] args )
    {
        SpringApplication.run(AppClassifier.class, args);//.close();
    }

    @KafkaListener(id = "aiGroup", topics = "topic1")
    public void listen(Command command) {
        //logger.info("Received: " + foo);
//        if (foo.getFoo().startsWith("fail")) {
//            throw new RuntimeException("failed");
//        }
        this.exec.execute(() -> System.out.println("Hit Enter to terminate..."));
    }

    @Bean
    public NewTopic topic() {
        return new NewTopic("topic1", 1, (short) 1);
    }

    @Bean
    @Profile("default") // Don't run from test(s)
    public ApplicationRunner runner() {
        return args -> {
            System.out.println("Hit Enter to terminate...");
            System.in.read();
        };
    }

}
