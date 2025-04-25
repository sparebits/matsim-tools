/*
 * RandomTimeTest
 * @author : neiko.neikov
 * @created : 18.03.25 г., Tuesday
 */
package com.sparebits.matsim;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

public class RandomTimeTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void distribution() {
        int numSamples = 50;
        double mean = 8 * 3600; // 8 AM in seconds
        double stdDev = 1800;   // 30 minutes in seconds
        NormalDistribution normalDist = new NormalDistribution(mean, stdDev);

        for (int i = 0; i < numSamples; i++) {
            int secondsSinceMidnight = (int) normalDist.sample();
            LocalTime time = LocalTime.ofSecondOfDay(secondsSinceMidnight);
            logger.info("departure time: {}", time);
        }
    }
}
