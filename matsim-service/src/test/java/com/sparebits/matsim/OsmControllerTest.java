/*
 * OsmControllerTest
 * @author : neiko.neikov
 * @created : 2.04.25 г., Wednesday
 */
package com.sparebits.matsim;

import com.sparebits.matsim.model.Node;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OsmController.class)
public class OsmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OsmService osmService;

    @Test
    public void testGetDrivableStreets() throws Exception {
        // Mock the service response
        List<Node> mockResponse = List.of(new Node(1, 7.0, 8.0));
        when(osmService.getDrivableStreets("10.0", "20.0", "30.0", "40.0"))
                .thenReturn(mockResponse);

        // Convert the mock response to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        // Perform the GET request and verify the response
        mockMvc.perform(get("/osm/nodes")
                        .param("south", "10.0")
                        .param("west", "20.0")
                        .param("north", "30.0")
                        .param("east", "40.0"))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResponse)); // Compare the response with the JSON string
    }
}