package com.abhi1kush.mockserver.dynamic;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mocks.dir=../mocks")
@AutoConfigureMockMvc
class DynamicMockControllerTests {
    @Autowired MockMvc mvc;

    @Test
    void echoReturnsRequestDetails() throws Exception {
        mvc.perform(put("/mock/dynamic/echo/a/b?x=1&x=2").queryParam("name", "hello world")
                        .contentType(MediaType.APPLICATION_JSON).header("X-Request-Id", "abc")
                        .header("X-Secret", "hidden").content("{\"k\":\"v\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.method").value("PUT"))
           .andExpect(jsonPath("$.path").value("/echo/a/b"))
           .andExpect(jsonPath("$.query.x", contains("1", "2")))
           .andExpect(jsonPath("$.query.name[0]").value("hello world"))
           .andExpect(jsonPath("$.headers['X-Request-Id']").value("abc"))
           .andExpect(jsonPath("$.headers['X-Secret']").doesNotExist())
           .andExpect(jsonPath("$.body.k").value("v"));
    }

    @Test
    void echoReturnsRawBodyWhenNotJson() throws Exception {
        mvc.perform(post("/mock/dynamic/echo").contentType(MediaType.TEXT_PLAIN).content("plain text"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.path").value("/echo"))
           .andExpect(jsonPath("$.body").value("plain text"));
    }

    @Test
    void userIsDerivedFromIdAndQuery() throws Exception {
        String first = mvc.perform(get("/mock/dynamic/users/42").queryParam("greeting", "Hi"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value("42"))
           .andExpect(jsonPath("$.name", not(emptyString())))
           .andExpect(jsonPath("$.email", endsWith(".42@example.com")))
           .andExpect(jsonPath("$.greeting", startsWith("Hi, ")))
           .andExpect(jsonPath("$.age", allOf(greaterThanOrEqualTo(18), lessThan(68))))
           .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(get("/mock/dynamic/users/42").queryParam("greeting", "Hi"))
           .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(first, second, "same id must yield same user");
    }

    @Test
    void userDefaultsGreetingAndRejectsPost() throws Exception {
        mvc.perform(get("/mock/dynamic/users/bob")).andExpect(status().isOk())
           .andExpect(jsonPath("$.greeting", startsWith("Hello, ")));
        mvc.perform(post("/mock/dynamic/users/bob")).andExpect(status().isNotFound());
    }

    @Test
    void orderComputesTotals() throws Exception {
        mvc.perform(post("/mock/dynamic/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"items":[{"sku":"A","price":9.99,"quantity":2},{"sku":"B","price":5,"quantity":1}],"taxRate":0.1}
                """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.orderId", startsWith("ORD-")))
           .andExpect(jsonPath("$.items[0].lineTotal").value(19.98))
           .andExpect(jsonPath("$.itemCount").value(3))
           .andExpect(jsonPath("$.subtotal").value(24.98))
           .andExpect(jsonPath("$.tax").value(2.50))
           .andExpect(jsonPath("$.total").value(27.48));
    }

    @Test
    void orderRejectsInvalidItems() throws Exception {
        mvc.perform(post("/mock/dynamic/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"sku\":\"\",\"price\":-1,\"quantity\":0}],\"taxRate\":2}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("Invalid order"))
           .andExpect(jsonPath("$.details", hasSize(4)));
    }

    @Test
    void orderRejectsMalformedJson() throws Exception {
        mvc.perform(post("/mock/dynamic/orders").contentType(MediaType.APPLICATION_JSON).content("{not json"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.details[0]").value("Body must be a JSON object"));
        mvc.perform(post("/mock/dynamic/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void noMatchListsAvailableHandlers() throws Exception {
        mvc.perform(delete("/mock/dynamic/unknown"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("No dynamic handler found"))
           .andExpect(jsonPath("$.method").value("DELETE"))
           .andExpect(jsonPath("$.path").value("/mock/dynamic/unknown"))
           .andExpect(jsonPath("$.availableHandlers[*].name", contains("EchoHandler", "UserHandler", "OrderHandler")))
           .andExpect(jsonPath("$.availableHandlers[1].path").value("/mock/dynamic/users/{id}"));
    }

    @Test
    void echoHandlesEveryMethod() throws Exception {
        for (var m : new org.springframework.http.HttpMethod[]{
                org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.POST,
                org.springframework.http.HttpMethod.PUT, org.springframework.http.HttpMethod.PATCH,
                org.springframework.http.HttpMethod.DELETE, org.springframework.http.HttpMethod.OPTIONS}) {
            mvc.perform(request(m, "/mock/dynamic/echo/m"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.method").value(m.name()));
        }
    }

    @Test
    void headOnGetHandlerReturnsHeadersOnly() throws Exception {
        mvc.perform(head("/mock/dynamic/users/42"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mvc.perform(head("/mock/dynamic/echo/h")).andExpect(status().isOk());
    }

    @Test
    void optionsReachesHandlerNotSpringDefault() throws Exception {
        mvc.perform(options("/mock/dynamic/echo/o"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.method").value("OPTIONS"));
        mvc.perform(options("/mock/dynamic/orders")).andExpect(status().isNotFound());
    }
}
