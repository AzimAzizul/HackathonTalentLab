package com.alekhya.ecommerce.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiAiClientTest {

    private final GeminiAiClient client = new GeminiAiClient(
            new AiProperties("key", "model", "http://localhost"), new ObjectMapper());

    @Test
    void extractTextJoinsAllPartsAndSkipsThoughts() {
        String response = """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[
                  {"text":"thinking...","thought":true},
                  {"text":"It is not designed for motor"},
                  {"text":"cycle riding."}
                ]}}]}""";

        assertThat(client.extractText(response)).isEqualTo("It is not designed for motorcycle riding.");
    }
}
