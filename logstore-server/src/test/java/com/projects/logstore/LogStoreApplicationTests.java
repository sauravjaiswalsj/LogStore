package com.projects.logstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "tablet.base-dir=target/logstore-spring-context-test/",
        "tablet.total-tablets=2"
})
class LogStoreApplicationTests {

    @Test
    void contextLoads() {
    }

}
