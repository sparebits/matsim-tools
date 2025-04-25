/*
 * NetworkComponent
 * @author : neiko.neikov
 * @created : 1.03.25 г., Saturday
 */
package com.sparebits.matsim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/network")
public class NetworkController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PopulationService populationService;

    @PostMapping
    public ResponseEntity<Void> network(@RequestParam("network") MultipartFile file) throws IOException {
        populationService.buildNetwork(file.getInputStream());
        return ResponseEntity.ok(null);
    }

}
