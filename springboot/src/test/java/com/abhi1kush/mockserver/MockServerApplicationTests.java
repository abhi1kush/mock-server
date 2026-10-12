package com.abhi1kush.mockserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mocks.dir=../mocks")
@AutoConfigureMockMvc
class MockServerApplicationTests {
    @Autowired MockMvc mvc;

    @Test
    void healthReturnsUp() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void exampleMockIsServed() throws Exception {
        mvc.perform(get("/api/hello")).andExpect(status().isOk())
           .andExpect(header().string("X-Mock", "true"))
           .andExpect(jsonPath("$.message").value("Hello from mock-server!"));
    }

    @Test
    void unmatchedReturns404Json() throws Exception {
        mvc.perform(post("/nope")).andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("No mock found"))
           .andExpect(jsonPath("$.path").value("/nope"));
    }
}
