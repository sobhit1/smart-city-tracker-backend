package com.project.smart_city_tracker_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/slow")
    public String slowResponse() throws InterruptedException {
        Thread.sleep(10000);
        return "Slow response received after 10 seconds";
    }
}
