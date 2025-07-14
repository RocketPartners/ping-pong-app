package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.dto.SystemStatsDTO;
import com.example.javapingpongelo.services.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private StatsService statsService;

    /**
     * API endpoint to get system statistics as JSON
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public SystemStatsDTO getSystemStats() {
        return statsService.getSystemStats();
    }
}