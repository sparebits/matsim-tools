/*
 * OsmToMatsimNetwork
 * @author : neiko.neikov
 * @created : 27.01.25 г., Monday
 */
package com.sparebits.matsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


/**
 * Sample converting OSM data file into
 */
public class OsmToMatsimNetwork {

    @Test
    @SuppressWarnings("deprecation")
    public void convert() {
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, "EPSG:3857"
//                TransformationFactory.WGS84, TransformationFactory.WGS84
        );
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        OsmNetworkReader reader = new OsmNetworkReader(network, ct);
        reader.parse("../routes-ext/src/test/netlogo/export.osm");
        new NetworkCleaner().run(network);
        new NetworkWriter(network).write("src/test/resources/network.xml");
    }
}
