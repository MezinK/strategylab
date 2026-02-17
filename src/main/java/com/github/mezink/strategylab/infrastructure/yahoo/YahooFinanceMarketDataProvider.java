package com.github.mezink.strategylab.infrastructure.yahoo;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Yahoo Finance market data provider using the v8 chart API.
 * Fetches daily candles via HTTP and parses the JSON response.
 */
public class YahooFinanceMarketDataProvider implements MarketDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YahooFinanceMarketDataProvider.class);
    private static final String BASE_URL = "https://query1.finance.yahoo.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YahooFinanceMarketDataProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end) {
        long period1 = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long period2 = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String json = fetchWithRetry(
                "/v8/finance/chart/{symbol}?period1={p1}&period2={p2}&interval=1d",
                symbol, period1, period2, 3
        );

        return parseChartResponse(symbol, json);
    }

    @Override
    public Optional<Instrument> validateSymbol(String symbol) {
        try {
            // Fetch a tiny range to confirm the symbol exists
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(7);
            long period1 = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long period2 = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            String json = fetchWithRetry(
                    "/v8/finance/chart/{symbol}?period1={p1}&period2={p2}&interval=1d",
                    symbol, period1, period2, 2
            );

            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("chart").path("result");
            if (result.isMissingNode() || !result.isArray() || result.isEmpty()) {
                return Optional.empty();
            }

            JsonNode meta = result.get(0).path("meta");
            String resolvedSymbol = meta.path("symbol").asString(symbol);
            String shortName = meta.path("shortName").asString(resolvedSymbol);
            String instrumentType = meta.path("instrumentType").asString("UNKNOWN");

            return Optional.of(new Instrument(resolvedSymbol, shortName, instrumentType));
        } catch (Exception e) {
            LOG.warn("Failed to validate symbol {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private String fetchWithRetry(String uriTemplate, String symbol, long period1, long period2, int maxAttempts) {
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                return restClient.get()
                        .uri(uriTemplate, symbol, period1, period2)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    throw new MarketDataFetchException(
                            "Failed to fetch data for %s after %d attempts: %s".formatted(symbol, maxAttempts, e.getMessage()), e);
                }
                LOG.warn("Attempt {}/{} failed for {}: {}. Retrying...", attempt, maxAttempts, symbol, e.getMessage());
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MarketDataFetchException("Interrupted during retry", ie);
                }
            }
        }
    }

    private TimeSeries parseChartResponse(String symbol, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("chart").path("result");

            if (result.isMissingNode() || !result.isArray() || result.isEmpty()) {
                throw new MarketDataFetchException("No chart data returned for " + symbol);
            }

            JsonNode firstResult = result.get(0);
            JsonNode meta = firstResult.path("meta");
            JsonNode timestamps = firstResult.path("timestamp");
            JsonNode indicators = firstResult.path("indicators").path("quote").get(0);

            if (timestamps.isMissingNode() || !timestamps.isArray()) {
                throw new MarketDataFetchException("No timestamps in response for " + symbol);
            }

            JsonNode adjCloses = resolveAdjClose(firstResult);
            List<Candle> candles = parseCandles(timestamps, indicators, adjCloses);

            if (candles.isEmpty()) {
                throw new MarketDataFetchException("No valid candles parsed for " + symbol);
            }

            String shortName = meta.path("shortName").asString(symbol);
            String instrumentType = meta.path("instrumentType").asString("UNKNOWN");
            Instrument instrument = new Instrument(symbol.toUpperCase(Locale.ROOT), shortName, instrumentType);

            return new TimeSeries(instrument, candles);
        } catch (MarketDataFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new MarketDataFetchException("Failed to parse Yahoo Finance response for " + symbol, e);
        }
    }

    private static JsonNode resolveAdjClose(JsonNode firstResult) {
        JsonNode adjCloseNode = firstResult.path("indicators").path("adjclose");
        if (!adjCloseNode.isMissingNode() && adjCloseNode.isArray() && !adjCloseNode.isEmpty()) {
            return adjCloseNode.get(0).path("adjclose");
        }
        return null;
    }

    private static List<Candle> parseCandles(JsonNode timestamps, JsonNode indicators, JsonNode adjCloses) {
        JsonNode opens = indicators.path("open");
        JsonNode highs = indicators.path("high");
        JsonNode lows = indicators.path("low");
        JsonNode closes = indicators.path("close");
        JsonNode volumes = indicators.path("volume");

        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (closes.get(i).isNull()) {
                continue;
            }

            long epochSeconds = timestamps.get(i).asLong();
            LocalDate date = Instant.ofEpochSecond(epochSeconds)
                    .atZone(ZoneId.of("America/New_York"))
                    .toLocalDate();

            BigDecimal open = toBigDecimal(opens.get(i));
            BigDecimal high = toBigDecimal(highs.get(i));
            BigDecimal low = toBigDecimal(lows.get(i));
            BigDecimal close = resolveClosePrice(closes.get(i), adjCloses, i);
            long volume = volumes.get(i).isNull() ? 0 : volumes.get(i).asLong();

            candles.add(new Candle(date, open, high, low, close, volume));
        }
        return candles;
    }

    private static BigDecimal resolveClosePrice(JsonNode closeNode, JsonNode adjCloses, int index) {
        if (adjCloses != null && !adjCloses.get(index).isNull()) {
            return toBigDecimal(adjCloses.get(index));
        }
        return toBigDecimal(closeNode);
    }

    private static BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(node.asDouble());
    }
}
