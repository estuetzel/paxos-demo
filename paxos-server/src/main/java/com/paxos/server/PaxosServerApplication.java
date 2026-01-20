package com.paxos.server;

import com.paxos.server.service.PaxosAcceptorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class PaxosServerApplication {

    private static final Logger log = LoggerFactory.getLogger(PaxosServerApplication.class);

    @Value("${paxos.server.count}")
    private int serverCount;

    public static void main(String[] args) {
        SpringApplication.run(PaxosServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application started serverCount={}", serverCount);
    }
}
