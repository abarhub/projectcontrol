package org.projectcontrol.core.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempUtilsTest {

    @Test
    void createTempFile() throws Exception {
        // ARRANGE

        // ACT
        var res = TempUtils.createTempFile("test-", ".tmp");

        // ASSERT
        assertNotNull(res);
        assertTrue(Files.exists(res));
        assertThat(res.getFileName().toString())
                .startsWith("test-")
                .endsWith(".tmp");
    }
}