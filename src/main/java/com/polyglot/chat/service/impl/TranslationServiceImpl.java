package com.polyglot.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polyglot.chat.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    // ⭐ Helsinki-NLP opus-translate Space (well-known research group, OPUS-MT models)
    @Value("${gradio.api.baseUrl:https://helsinki-nlp-opus-translate.hf.space}")
    private String baseUrl;

    // Optional — this Space doesn't appear to require auth, but harmless to keep
    // in case it starts rate-limiting anonymous requests later.
    @Value("${huggingface.api.token:}")
    private String hfToken;

    @Value("${translation.use.mock:false}")
    private boolean useMockTranslation;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ⭐ This Space expects language values in the format "code (Name)",
    // e.g. "en (English)", "hi (Hindi)". We only need to store the display
    // name here; the code is already your app's own language key.
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<>();

    static {
        LANGUAGE_NAMES.put("en", "English");
        LANGUAGE_NAMES.put("es", "Spanish");
        LANGUAGE_NAMES.put("fr", "French");
        LANGUAGE_NAMES.put("de", "German");
        LANGUAGE_NAMES.put("it", "Italian");
        LANGUAGE_NAMES.put("pt", "Portuguese");
        LANGUAGE_NAMES.put("nl", "Dutch");
        LANGUAGE_NAMES.put("pl", "Polish");
        LANGUAGE_NAMES.put("sv", "Swedish");
        LANGUAGE_NAMES.put("tr", "Turkish");
        LANGUAGE_NAMES.put("ja", "Japanese");
        LANGUAGE_NAMES.put("zh", "Chinese");
        LANGUAGE_NAMES.put("ko", "Korean");
        LANGUAGE_NAMES.put("hi", "Hindi");
        LANGUAGE_NAMES.put("ar", "Arabic");
        LANGUAGE_NAMES.put("ru", "Russian");
        LANGUAGE_NAMES.put("th", "Thai");
        LANGUAGE_NAMES.put("vi", "Vietnamese");
        LANGUAGE_NAMES.put("id", "Indonesian");
        LANGUAGE_NAMES.put("ne", "Nepali");
        LANGUAGE_NAMES.put("bn", "Bengali");
        LANGUAGE_NAMES.put("ta", "Tamil");
        LANGUAGE_NAMES.put("mr", "Marathi");
        // Note: not all of these are guaranteed to be in this Space's supported
        // list. Unsupported ones will simply fail per-request and fall back to
        // original text via the existing safety net below — no crash either way.
    }

    // Builds the plain language name this Space expects, e.g. "Hindi"
    private String toGradioLanguageFormat(String code) {
        return LANGUAGE_NAMES.get(code);
    }

    @Override
    public Map<String, String> translateText(String text, String sourceLanguage, Set<String> targetLanguages) {
        Map<String, String> translations = new ConcurrentHashMap<>();

        log.info("🌍 Translating text from {} to {}: '{}'", sourceLanguage, targetLanguages, text);

        if (text == null || text.trim().isEmpty()) {
            log.warn("⚠️ Empty text provided, returning empty for all languages");
            for (String targetLang : targetLanguages) {
                translations.put(targetLang, text);
            }
            return translations;
        }

        if (!LANGUAGE_NAMES.containsKey(sourceLanguage)) {
            log.warn("⚠️ Source language '{}' not supported, returning original text", sourceLanguage);
            for (String targetLang : targetLanguages) {
                translations.put(targetLang, text);
            }
            return translations;
        }

        if (useMockTranslation) {
            log.info("🎭 Using MOCK translation (translation.use.mock=true)");
            return mockTranslations(text, sourceLanguage, targetLanguages);
        }

        log.info("🤖 Using REAL Gradio (TranslateGemma) translation via Third-Party Space");
        log.info("🔗 Base URL: {}", baseUrl);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(targetLanguages.size(), 5));

        for (String targetLang : targetLanguages) {
            if (targetLang.equals(sourceLanguage)) {
                log.info("⏭️ Skipping translation from {} to {} (same language)", sourceLanguage, targetLang);
                translations.put(targetLang, text);
                continue;
            }

            if (!LANGUAGE_NAMES.containsKey(targetLang)) {
                log.warn("⚠️ Target language '{}' not supported, using original text", targetLang);
                translations.put(targetLang, text);
                continue;
            }

            executor.submit(() -> {
                try {
                    String translatedText = translateWithGradioSpace(text, sourceLanguage, targetLang);

                    if (translatedText != null && !translatedText.equals(text)) {
                        log.info("✅ Translation successful: '{}' ({}) -> '{}' ({})",
                                text, sourceLanguage, translatedText, targetLang);
                        translations.put(targetLang, translatedText);
                    } else {
                        log.warn("⚠️ Translation failed or returned same text for {}, using original", targetLang);
                        translations.put(targetLang, text);
                    }
                } catch (Exception e) {
                    log.error("❌ Error translating to {}: {}", targetLang, e.getMessage(), e);
                    translations.put(targetLang, text); // Fallback to original text
                }
            });
        }

        executor.shutdown();
        try {
            boolean finished = executor.awaitTermination(90, TimeUnit.SECONDS);
            if (!finished) {
                log.error("❌ Translation timeout! Some translations may be incomplete.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("❌ Translation executor interrupted", e);
            Thread.currentThread().interrupt();
        }

        log.info("📊 Translation complete. Results: {}", translations);
        return translations;
    }

    // ⭐ Step 1 (POST) + Step 2 (GET/SSE) Gradio call flow, with retry
    private String translateWithGradioSpace(String text, String sourceLang, String targetLang) {
        String sourceValue = toGradioLanguageFormat(sourceLang);
        String targetValue = toGradioLanguageFormat(targetLang);

        log.info("🔄 Gradio Translation: '{}' from {} to {}", text, sourceValue, targetValue);

        try {
            String eventId = submitTranslationJob(text, sourceValue, targetValue)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                    .block();

            if (eventId == null || eventId.isBlank()) {
                log.error("❌ No event_id returned from Gradio POST request");
                return text;
            }

            log.info("📤 Submitted job, event_id: {}", eventId);

            String rawStream = fetchTranslationResult(eventId)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                    .block();

            return extractTranslationFromSse(rawStream, text);

        } catch (Exception e) {
            log.error("❌ Exception calling Gradio Space: {} - {}", e.getClass().getName(), e.getMessage());
            return text; // fallback to original
        }
    }

    private Mono<String> submitTranslationJob(String text, String sourceValue, String targetValue) {
        // Params in function-signature order: text, source, target
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", java.util.List.of(text, sourceValue, targetValue));

        // ⭐ This Space uses "/call/translate" — NOT "/gradio_api/call/translate"
        String postUrl = baseUrl + "/call/translate";
        log.info("📤 POST {}", postUrl);
        log.info("🔑 HF token present: {}", hfToken != null && !hfToken.isBlank());

        WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(postUrl)
                .contentType(MediaType.APPLICATION_JSON);

        if (hfToken != null && !hfToken.isBlank()) {
            requestSpec = (WebClient.RequestBodySpec) requestSpec.header("Authorization", "Bearer " + hfToken.trim());
        }

        return requestSpec
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ Error submitting job to Gradio Space: {}", errorBody);
                                    return Mono.error(new RuntimeException("Gradio submit error: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .map(this::parseEventId);
    }

    private String parseEventId(String responseBody) {
        try {
            log.info("📥 POST response: {}", responseBody);
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("event_id")) {
                return node.get("event_id").asText();
            }
            return responseBody.replaceAll("[\"{}]", "").trim();
        } catch (Exception e) {
            log.error("❌ Failed to parse event_id from response: {}", responseBody);
            return null;
        }
    }

    private Mono<String> fetchTranslationResult(String eventId) {
        // ⭐ Matches "/call/translate" used above
        String getUrl = baseUrl + "/call/translate/" + eventId;
        log.info("📥 GET (SSE) {}", getUrl);

        WebClient.RequestHeadersSpec<?> getSpec = webClient.get()
                .uri(getUrl)
                .accept(MediaType.TEXT_EVENT_STREAM);

        boolean tokenAttached = hfToken != null && !hfToken.isBlank();
        log.info("🔑 GET request — HF token present: {}", tokenAttached);

        if (tokenAttached) {
            getSpec = getSpec.header("Authorization", "Bearer " + hfToken.trim());
        }

        return getSpec
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ Error fetching result from Gradio Space: {}", errorBody);
                                    return Mono.error(new RuntimeException("Gradio fetch error: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(45));
    }

    // ⭐ Parses the raw SSE stream text and extracts the translated string
    // from the final "data: [...]" event line.
    private String extractTranslationFromSse(String rawStream, String originalText) {
        if (rawStream == null || rawStream.isBlank()) {
            log.error("❌ Empty SSE stream from Gradio Space");
            return originalText;
        }

        log.info("📥 Raw SSE stream: {}", rawStream);

        String lastDataLine = null;
        for (String line : rawStream.split("\\R")) {
            if (line.startsWith("data:")) {
                lastDataLine = line.substring("data:".length()).trim();
            }
        }

        if (lastDataLine == null) {
            log.error("❌ No 'data:' line found in SSE stream");
            return originalText;
        }

        try {
            JsonNode arrayNode = objectMapper.readTree(lastDataLine);
            if (arrayNode.isArray() && arrayNode.size() > 0) {
                String translated = arrayNode.get(0).asText().trim();
                if (!translated.isEmpty()) {
                    return translated;
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to parse final data line '{}': {}", lastDataLine, e.getMessage());
        }

        return originalText;
    }

    // Mock translation method for development/testing
    private Map<String, String> mockTranslations(String text, String sourceLanguage, Set<String> targetLanguages) {
        Map<String, String> translations = new ConcurrentHashMap<>();
        log.info("🎭 Using mock translations for testing");

        for (String targetLang : targetLanguages) {
            if (targetLang.equals(sourceLanguage)) {
                translations.put(targetLang, text);
                continue;
            }
            String translatedText = mockTranslate(text, sourceLanguage, targetLang);
            translations.put(targetLang, translatedText);
            log.debug("🎭 Mock translation: {} -> {} = '{}'", sourceLanguage, targetLang, translatedText);
        }
        return translations;
    }

    private String mockTranslate(String text, String sourceLang, String targetLang) {
        Map<String, String> greetings = new HashMap<>();
        greetings.put("en", "Hello");
        greetings.put("es", "Hola");
        greetings.put("fr", "Bonjour");
        greetings.put("de", "Guten Tag");
        greetings.put("it", "Ciao");
        greetings.put("ja", "こんにちは");
        greetings.put("zh", "你好");
        greetings.put("ru", "Привет");
        greetings.put("ar", "مرحبا");
        greetings.put("hi", "नमस्ते");
        greetings.put("pt", "Olá");
        greetings.put("nl", "Hallo");
        greetings.put("ko", "안녕하세요");
        greetings.put("tr", "Merhaba");
        greetings.put("ta", "வணக்கம்");
        greetings.put("ne", "नमस्ते");

        String greeting = greetings.getOrDefault(targetLang, "Hello");
        return "[" + targetLang.toUpperCase() + "] " + greeting + " - " + text;
    }
}