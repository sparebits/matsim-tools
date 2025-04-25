/*
 * PopulationGenerator
 * @author : neiko.neikov
 * @created : 1.02.25 г., Saturday
 */
package com.sparebits.matsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class PopulationGenerator {

    private static final Config config = ConfigUtils.loadConfig(PopulationGenerator.class.getResource("/config.xml"));

    @Test
    public void generate() {

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("src/test/resources/network.xml");

        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        // Create a new person
        Person person = factory.createPerson(Id.createPersonId("1"));
        Plan plan = factory.createPlan();

        // Select random nodes for activity locations
        Node homeNode = network.getNodes().get(Id.createNodeId("267838917")); // First node
        Node workNode = network.getNodes().get(Id.createNodeId("1301864454")); // Example: Replace with better logic

        // Create home activity at a node in the network
        Activity home = factory.createActivityFromCoord("home", homeNode.getCoord());
        home.setLinkId(homeNode.getOutLinks().values().iterator().next().getId());
        home.setEndTime(8 * 3600); // Leave home at 8:00 AM
        plan.addActivity(home);

        // Create a leg (the trip)
        Leg leg = factory.createLeg(TransportMode.car);
        NetworkRoute route = generateRoute(homeNode, workNode, network, person);
        route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, network));
        route.setTravelTime(780);
        leg.setRoute(route);
        plan.addLeg(leg);

        // Create work activity at another node in the network
        Activity work = factory.createActivityFromCoord("work", workNode.getCoord());
        work.setLinkId(workNode.getOutLinks().values().iterator().next().getId());
        work.setEndTime(17 * 3600); // Leave work at 5:00 PM
        plan.addActivity(work);

        // Add plan to person and person to population
        person.addPlan(plan);
        population.addPerson(person);

        // Write the generated population to a file
        String plansFile = "src/test/resources/gen-plans.xml";
        new PopulationWriter(population).write(plansFile);
    }


    private static class MyDijkstra extends Dijkstra {
        public MyDijkstra(Network network) {
            super(network, null, null);
        }

        public MyDijkstra(Network network, TravelDisutility travelDisutility, TravelTime travelTime) {
            super(network, travelDisutility, travelTime);
        }
    }


    private NetworkRoute generateRoute(Node startNode, Node endNode, Network network, Person person) {
        // Create a default travel time calculator
        TravelTime travelTime = new FreeSpeedTravelTime();

        // Use a standard travel disutility function (e.g., time-based cost)
        TravelDisutilityFactory travelDisutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
        TravelDisutility travelDisutility = new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength();
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return 0;
            }
        };

        // Use Dijkstra's algorithm to find the shortest route
        LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
        LeastCostPathCalculator.Path path = router.calcLeastCostPath(startNode, endNode, 0, person, null);

        // Extract link sequence from path
        List<Id<Link>> linkIds = new ArrayList<>();
        for (Link link : path.links) {
            linkIds.add(link.getId());
        }

        // Create and return a NetworkRoute
        return RouteUtils.createNetworkRoute(linkIds);
    }
}
