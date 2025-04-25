/*
 * PlanController
 * @author : neiko.neikov
 * @created : 3.03.25 г., Monday
 */
package com.sparebits.matsim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparebits.matsim.model.Node;
import com.sparebits.matsim.model.Route;
import jakarta.servlet.http.HttpServletResponse;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/plan")
public class PopulationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PopulationService populationService;

    @Autowired
    private ObjectMapper mapper;

    private List<Route> routes;

    public record PlanRequest(Node from, Node to) {
    }

    @PostMapping
    public ResponseEntity<Route> plan(@RequestBody PlanRequest planRequest) {
        logger.info("building plan for {} to {}", planRequest.from, planRequest.to);
        Route route = populationService.buildPlan(planRequest.from.id(), planRequest.to.id());
        return ResponseEntity.ok(route);
    }

    @GetMapping
    public void download(HttpServletResponse response) throws IOException {
        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=\"plans.xml\"");
        new PopulationWriter(populationService.getPopulation()).write(response.getOutputStream());
    }

    @GetMapping("/random")
    public ResponseEntity<Route> randomPlan(@RequestParam(required = false, defaultValue = "1") int number) {
        logger.info("number of plans requested {}", number);
        Route route = populationService.randomPlan();
        return ResponseEntity.ok(route);
    }

    //    @GetMapping(value = "/random-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter random(@RequestParam(required = false, defaultValue = "1") int number) {
        SseEmitter emitter = new SseEmitter(0L);
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                for (int i = 0; i < number; i++) {
                    String route = mapper.writeValueAsString(populationService.randomPlan());
                    emitter.send(SseEmitter.event().name("route").data(route));
                    TimeUnit.SECONDS.sleep(1);
                }
                emitter.complete();
            } catch (IOException | InterruptedException ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private String asString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "/random-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> randomFlux(@RequestParam(required = false, defaultValue = "1") int number) {
        return Flux.range(1, number).buffer(1)
                .map(sequence -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(sequence))
                        .event("periodic-event")
                        .data(asString(populationService.randomPlan()))
                        .build())
                .onErrorResume(ex -> {
                    ErrorResponse error = new ErrorResponse("An unexpected error occurred.", ex.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(asString(error))
                            .build());
                });
    }
}
