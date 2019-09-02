package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher;

import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jobs.QueueViewingActivityJob;
import org.quartz.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NetflixhistoryWatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetflixhistoryWatcherApplication.class, args);
    }

    @Bean
    public JobDetail netflixViewingHistoryPersistJobDetail() {
        return JobBuilder.newJob(QueueViewingActivityJob.class).withIdentity("queueViewingActivity")
                .usingJobData("name", "World").storeDurably().build();
    }

    @Bean
    public Trigger netflixViewingHistoryPersistJobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(1)
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(netflixViewingHistoryPersistJobDetail())
                .withIdentity("queueViewingActivityTrigger")
                .withSchedule(scheduleBuilder).build();
    }

}
