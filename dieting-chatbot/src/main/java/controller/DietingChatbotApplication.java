/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package controller;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import reactor.Environment;
import reactor.bus.EventBus;

/**
 * Spring boot application wrapper for the system.
 * @author szhouan
 * @version v1.0.0
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"controller", "agent", "misc", "database", "utility"})
public class DietingChatbotApplication {

    /**
     * This method is used to initialize the environment.
     * @return Environment.
     */
    @Bean
    Environment env() {
        return Environment.initializeIfEmpty()
                          .assignErrorJournal();
    }

    /**
     * This method is used to create eventBus.
     * @return EventBus.
     */
    @Bean
    EventBus createEventBus(Environment env) {
	    return EventBus.create(env, Environment.THREAD_POOL);
    }

    /**
     * This method is used to create task scheduler.
     * @return TaskScheduler.
     */
    @Bean
    TaskScheduler createTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    /**
     * Path to downloaded contents.
     */
    static Path downloadedContentDir;

    /**
     * Static attribute staticPath Path object.
     */
    static Path staticPath;
    static {
        try {
            staticPath = Files.createDirectory(Paths.get("static")); 
        }
        catch(FileAlreadyExistsException e) {
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry point of the application.
     * @param args String array of arguments.
     * @throws IOException if error occurs in handling I/O.
     */
    public static void main(String[] args) throws IOException {
        downloadedContentDir = Files.createTempDirectory("line-bot");
        SpringApplication.run(DietingChatbotApplication.class, args);
    }
}