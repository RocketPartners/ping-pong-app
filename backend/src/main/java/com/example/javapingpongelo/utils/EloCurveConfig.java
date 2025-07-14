package com.example.javapingpongelo.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "elo.curve")
public class EloCurveConfig {
    // Master switch to enable/disable all curves
    private boolean enabled = true;

    // Upper threshold - normal Elo above this
    private int upperThreshold = 1600;

    // Lower threshold - extra loss protection below this
    private int lowerThreshold = 1200;

    // How much to boost gains below upperThreshold (1.2 = 20% boost)
    private double gainBoostFactor = 1.2;

    // How much to reduce losses below lowerThreshold (0.8 = 20% reduction)
    private double lossReductionFactor = 0.8;
}