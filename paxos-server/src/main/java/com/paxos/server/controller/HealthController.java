package com.paxos.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/health")
public class HealthController {

//    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /**
     * Health check endpoint
     */
    @GetMapping("")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Server is healthy");
    }
}
