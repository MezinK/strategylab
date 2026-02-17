package com.github.mezink.strategylab.domain.strategy.config;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuration for the Dollar Cost Averaging strategy.
 *
 * @param contributionAmount dollar amount to invest each period
 * @param frequencyDays      number of trading days between contributions
 */
public record DcaConfig(
        BigDecimal contributionAmount,
        int frequencyDays
) implements StrategyConfig {

    private static final int INITIAL_TRADE_COUNT = 1;

    public DcaConfig {
        if (contributionAmount == null || contributionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("contributionAmount must be a positive number");
        }
        if (frequencyDays <= 0) {
            throw new IllegalArgumentException("frequencyDays must be a positive integer");
        }
    }

    /**
     * Parse from raw request parameters. Throws on missing or invalid values.
     */
    public static DcaConfig fromParams(Map<String, String> params) {
        String amountStr = params.get("contributionAmount");
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: contributionAmount");
        }

        String freqStr = params.get("frequencyDays");
        if (freqStr == null || freqStr.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: frequencyDays");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("contributionAmount must be a valid number: " + amountStr);
        }

        int freq;
        try {
            freq = Integer.parseInt(freqStr);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("frequencyDays must be a valid integer: " + freqStr);
        }

        return new DcaConfig(amount, freq);
    }

    @Override
    public BigDecimal totalContributions(BigDecimal initialCapital, int tradeCount) {
        if (tradeCount <= INITIAL_TRADE_COUNT) {
            return initialCapital;
        }
        long dcaContributions = (long) tradeCount - INITIAL_TRADE_COUNT;
        return initialCapital.add(contributionAmount.multiply(BigDecimal.valueOf(dcaContributions)));
    }
}
