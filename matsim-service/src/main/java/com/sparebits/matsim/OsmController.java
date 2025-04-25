package com.sparebits.matsim;

import com.sparebits.matsim.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/osm")
public class OsmController {

    @Autowired
    private OsmService osmService;

    @GetMapping("/nodes")
    public List<Node> nodes(
            @RequestParam String south,
            @RequestParam String west,
            @RequestParam String north,
            @RequestParam String east
    ) {
        return osmService.getDrivableStreets(south, west, north, east);
    }
}