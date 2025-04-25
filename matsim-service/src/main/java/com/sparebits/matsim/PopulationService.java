/*
 * @author : neiko.neikov
 * @created : 3.3.2025 г., понеделник
 */
package com.sparebits.matsim;

import com.sparebits.matsim.model.Route;
import jakarta.annotation.PostConstruct;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Service
public class PopulationService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Config config;
    private Scenario scenario;
    private Network network;
    private PopulationFactory factory;
    private long personId;

    // random generation parameters
    // FIXME configurable ranges for randomisation
    private double meanHome = 7 * 3600 + 45 * 60;   // rush hour is 7:45
    private double stdDevHome = 1800;   // 68% of the traffix in the morning is within one hour around the rush hour
    private NormalDistribution normalDistHome;
    private double meanWork = 17 * 3600 + 15 * 60;   // rush hour is 17:15
    private double stdDevWork = 1800;   // 68% of the traffix in the morning is within one hour around the rush hour
    private NormalDistribution normalDistWork;

    @PostConstruct
    private void setup() {
        logger.info("initializing network component...");
        config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.network = scenario.getNetwork();
        this.factory = this.scenario.getPopulation().getFactory();
        this.normalDistHome = new NormalDistribution(meanHome, stdDevHome);
        this.normalDistWork = new NormalDistribution(meanWork, stdDevWork);
    }

    public Scenario getScenario() {
        return scenario;
    }

    /**
     * Add network data to scenario for further building plans
     *
     * @param is is the input stream used to build the MATSim network
     */
    public void buildNetwork(InputStream is) {
        logger.info("building network...");
        this.scenario = ScenarioUtils.createScenario(config);
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readStream(is);
        this.network = scenario.getNetwork();
    }

    public void buildNetworkFromOsm(String osm) {
        this.scenario = ScenarioUtils.createScenario(config);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, "EPSG:3857"
        );
        OsmNetworkReader reader = new OsmNetworkReader(scenario.getNetwork(), ct);
        reader.parse(() -> new ByteArrayInputStream(osm.getBytes()));
        new NetworkCleaner().run(scenario.getNetwork());
        this.network = scenario.getNetwork();
    }

    /**
     * Builds plan provided start and end nodes on the map
     * @param fromNodeId starting node in the plan
     * @param toNodeId final node in the plan
     */
    public Route buildPlan(long fromNodeId, long toNodeId) {
        // generate the route used for the movement
        Node homeNode = network.getNodes().get(Id.createNodeId(fromNodeId));    // from node
        Node workNode = network.getNodes().get(Id.createNodeId(toNodeId));      // to node
        return buildPlan(homeNode, workNode);
    }

    public Route buildPlan(Node homeNode, Node workNode) {
        NetworkRoute route = generateRoute(homeNode, workNode, network);
        logger.info("route: {}", route);
        route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, network));

        // create random population on this route
        Person person = factory.createPerson(Id.createPersonId(++personId));
        Plan plan = factory.createPlan();

        // Create home activity at a node in the network
        Activity home = factory.createActivityFromCoord("home", homeNode.getCoord());
        home.setLinkId(homeNode.getOutLinks().values().iterator().next().getId());
        home.setEndTime(normalDistHome.sample()); // Leave home around 7:45 AM
        plan.addActivity(home);

        // Create a leg (the trip)
        Leg leg = factory.createLeg(TransportMode.car);
        leg.setRoute(route);
        plan.addLeg(leg);

        // Create work activity at another node in the network
        Activity work = factory.createActivityFromCoord("work", workNode.getCoord());
        work.setLinkId(workNode.getOutLinks().values().iterator().next().getId());
        work.setEndTime(normalDistWork.sample()); // Leave work around 5:15 PM
        plan.addActivity(work);

        // Add plan to person and person to population
        person.addPlan(plan);
        this.scenario.getPopulation().addPerson(person);
        return toModelRoute(route);
    }

    /**
     * Convert MATSim network route to this service model route
     *
     * @param networkRoute MATSim network route to be transferred
     * @return this service model route
     */
    private Route toModelRoute(NetworkRoute networkRoute) {
        Route route = new Route(new ArrayList<>());
        Link startLink = network.getLinks().get(networkRoute.getStartLinkId());
        route.links().add(new com.sparebits.matsim.model.Link(
                Long.parseLong(startLink.getId().toString()),
                toModelNode(startLink.getFromNode()),
                toModelNode(startLink.getToNode())
        ));
        networkRoute.getLinkIds().forEach(linkId -> {
            Link matsimLink = network.getLinks().get(linkId);
            com.sparebits.matsim.model.Link link = new com.sparebits.matsim.model.Link(
                    Long.parseLong(matsimLink.getId().toString()),
                    toModelNode(matsimLink.getFromNode()),
                    toModelNode(matsimLink.getToNode())
            );
            route.links().add(link);
        });
        if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())) {
            Link endLink = network.getLinks().get(networkRoute.getEndLinkId());
            route.links().add(new com.sparebits.matsim.model.Link(
                    Long.parseLong(endLink.getId().toString()),
                    toModelNode(endLink.getFromNode()),
                    toModelNode(endLink.getToNode())
            ));
        }
        return route;
    }

    private com.sparebits.matsim.model.Node toModelNode(Node matsimNode) {
        return new com.sparebits.matsim.model.Node(
                Long.parseLong(matsimNode.getId().toString()),
                matsimNode.getCoord().getX(),
                matsimNode.getCoord().getY());
    }

    private NetworkRoute generateRoute(Node startNode, Node endNode, Network network) {
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
        LeastCostPathCalculator.Path path = router.calcLeastCostPath(startNode, endNode, 0, null, null);

        // Extract link sequence from path
        List<Id<Link>> linkIds = new ArrayList<>();
        for (Link link : path.links) {
            linkIds.add(link.getId());
        }

        // Create and return a NetworkRoute
        return RouteUtils.createNetworkRoute(linkIds);
    }

    public Population getPopulation() {
        return this.scenario.getPopulation();
    }

    public List<com.sparebits.matsim.model.Node> getNetworkNodes() {
        return this.scenario.getNetwork().getNodes().values().stream().map(this::toModelNode).collect(Collectors.toList());
    }

    public Route randomPlan() {
        List<Id<Node>> keys = List.copyOf(this.scenario.getNetwork().getNodes().keySet());
        Id<Node> start = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        Id<Node> end = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        return buildPlan(
                scenario.getNetwork().getNodes().get(start),
                scenario.getNetwork().getNodes().get(end)
        );
    }
}
