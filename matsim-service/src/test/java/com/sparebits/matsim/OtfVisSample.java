/*
 * OtfVisSample
 * @author : neiko.neikov
 * @created : 28.01.25 г., Tuesday
 */
package com.sparebits.matsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisGUI;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.snapshotwriters.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Code in this sample taken from: https://github.com/matsim-org/matsim-libs/blob/master/contribs/otfvis/src/main/java/org/matsim/contrib/otfvis/OTFVis.java
 * But doesn't seem to work properly. The same result is observed when loading network file in OTFVisGUI dialog
 */
public class OtfVisSample {

    private static final String filename = "/home/neyko/Projects/Thesis/gis-occupancy/matsim-sample/input/network.xml";

    @Test
    public void otfvis() {
        OTFVisGUI.runDialog();
    }

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(filename);
        EventsManager events = EventsUtils.createEventsManager();
        final Map<Id<Link>, VisLink> visLinks = new HashMap<>();
        for (final Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
            visLinks.put(linkId, new VisLink() {
                @Override
                public Link getLink() {
                    return scenario.getNetwork().getLinks().get(linkId);
                }

                @Override
                public Collection<? extends VisVehicle> getAllVehicles() {
                    return Collections.emptyList();
                }

                @Override
                public VisData getVisData() {
                    return positions -> Collections.emptyList();
                }
            });
        }
        OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events, new VisMobsim() {
            @Override
            public void run() {

            }

            @Override
            public void addQueueSimulationListeners(MobsimListener listener) {

            }

            @Override
            public VisNetwork getVisNetwork() {
                return new VisNetwork() {
                    @Override
                    public Map<Id<Link>, ? extends VisLink> getVisLinks() {
                        return visLinks;
                    }

                    @Override
                    public Network getNetwork() {
                        return scenario.getNetwork();
                    }
                };
            }

            @Override
            public Map<Id<Person>, MobsimAgent> getAgents() {
                return Collections.emptyMap();
            }

            @Override
            public VisData getNonNetworkAgentSnapshots() {
                return positions -> Collections.emptyList();
            }
        });

        OTFClientLive.run(config, server);
    }
}
