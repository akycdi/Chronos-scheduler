package com.chronos.service;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.cronutils.model.CronType.QUARTZ;

@Slf4j
@Service
public class ScheduleService {

    private final CronParser cronParser;

    public ScheduleService() {
        this.cronParser = new CronParser(
                com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    }

    /**
     * Calculate the next run time from a cron expression
     */
    public Optional<LocalDateTime> getNextRunTime(String cronExpression, LocalDateTime from) {
        try {
            Cron cron = cronParser.parse(cronExpression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime fromZoned = from.atZone(ZoneId.systemDefault());
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(fromZoned);

            return nextExecution.map(zdt -> zdt.toLocalDateTime());
        } catch (Exception e) {
            log.error("Error parsing cron expression: {}", cronExpression, e);
            return Optional.empty();
        }
    }

    /**
     * Calculate the next run time from a cron expression starting from now
     */
    public Optional<LocalDateTime> getNextRunTime(String cronExpression) {
        return getNextRunTime(cronExpression, LocalDateTime.now());
    }

    /**
     * Validate cron expression
     */
    public boolean isValidCronExpression(String cronExpression) {
        try {
            cronParser.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse ISO datetime string to LocalDateTime
     */
    public Optional<LocalDateTime> parseDateTime(String dateTimeString) {
        try {
            return Optional.of(LocalDateTime.parse(dateTimeString));
        } catch (Exception e) {
            log.error("Error parsing datetime: {}", dateTimeString, e);
            return Optional.empty();
        }
    }
}
