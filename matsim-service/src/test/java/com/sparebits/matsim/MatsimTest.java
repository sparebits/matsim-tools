/*
 * MatsimTest
 * @author : neiko.neikov
 * @created : 24.01.25 г., Friday
 */
package com.sparebits.matsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.MalformedURLException;


public class MatsimTest {

    @Test
    public void sample() throws MalformedURLException {
        Config config = ConfigUtils.loadConfig(getClass().getResource("/config.xml"));
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();
    }
}
