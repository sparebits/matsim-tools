package com.sparebits.matsim;

import com.sparebits.matsim.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Service
public class OsmService {

    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PopulationService populationService;

    @Autowired
    public OsmService(PopulationService populationService) {
        this.populationService = populationService;
    }

    public List<Node> getDrivableStreets(String south, String west, String north, String east) {
        // Construct the Overpass API query
        String query = String.format(
                "[out:xml];" +
                        "(way[\"highway\"~\"motorway|trunk|primary|secondary|tertiary|unclassified|residential\"](%s,%s,%s,%s););" +
                        "out body; >; out skel qt;",
                south, west, north, east
        );

        // Send the query to Overpass API
        RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8)).build();
        String url = OVERPASS_API_URL + "?data=" + query;
        String response = restTemplate.getForObject(url, String.class);

        populationService.buildNetworkFromOsm(response);
        return populationService.getNetworkNodes();
    }

}