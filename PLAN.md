# PLAN.md — Backtesting Platform Prototype (Yahoo Finance + Hardcoded Strategies)

## 0) Goal (Prototype Scope)
Build a small backtesting platform that lets a user:
1) Select one or more instruments (tickers) sourced from Yahoo Finance  
2) Pick a strategy (hardcoded for now) and parameters (start date, capital, frequency, etc.)  
3) Run a backtest and compare results to:
   - Buy & Hold on the same instrument
   - SPY DCA baseline
   - SPY Buy & Hold baseline

Prototype-first: correctness, clarity, and a clean internal architecture matter more than feature breadth. We will keep data frequency to **daily candles** and ignore intraday, corporate actions edge cases, and advanced order types.

(We follow the “build a working prototype in iterative increments + clean architecture / maintainable code” mindset from the course kickoff, but we’re not blocking ourselves with “no frameworks” rules.) :contentReference[oaicite:0]{index=0}


---

## 1) Core Use Cases
### UC1 — SelectInstrument
- User searches/inputs ticker(s): e.g., AAPL, MSFT, BTC-USD, SPY
- System validates ticker (exists / fetchable)
- System fetches & caches historical price series

### UC2 — SelectStrategyForInstrument
- User selects a strategy and config:
  - start/end date
  - initial capital
  - recurring contribution (if relevant)
  - strategy parameters (window length, threshold, etc.)

### UC3 — CompareToBaselines
- Runs chosen strategy
- Runs baselines on same timeframe
- Produces comparison metrics + equity curves

### UC4 — ViewResults
- Equity curve
- Trades (if applicable)
- Metrics table (CAGR, max drawdown, volatility, Sharpe (rf=0), final value, contributions, etc.)

---

## 2) Strategies (Hardcoded Set: 3–4)
Pick **3** for minimal scope, add the 4th if time allows.

### S1 — DCA (Dollar Cost Averaging)
- Buy fixed amount every N trading days (e.g., weekly/monthly)
- No selling
- Parameters: contributionAmount, frequency (weekly/monthly), allowFractionalShares (true for prototype)

### S2 — Buy & Hold (baseline + usable strategy)
- Invest all initial capital at start date, then hold
- Optional: add contributions monthly (off by default)

### S3 — Moving Average Crossover
- Signal:
  - if SMA(short) > SMA(long): fully invested
  - else: fully in cash
- Parameters: shortWindow, longWindow
- Trades only on signal changes (to avoid daily churn)

### S4 (optional) — Mean Reversion to SMA
- If price < SMA(window) * (1 - threshold): buy (go invested)
- If price > SMA(window) * (1 + threshold): sell (go cash)
- Parameters: window, threshold

Constraints for prototype:
- Long-only (no shorting)
- Market orders at close price of signal day (simple + deterministic)
- No slippage, no fees (or flat fee per trade if you want a switch)
- Fractional shares allowed (makes DCA clean)

---

## 3) Non-Goals (Explicitly Out of Scope)
- Intraday backtesting, limit orders, partial fills
- Corporate actions handling (splits/dividends) beyond “adjusted close” if available
- Portfolio optimization, multi-asset allocations (start single-asset, then add multi-asset later)
- User accounts, auth, payments
- “Real trading” execution integration (broker APIs)

---

## 4) Architecture (Clean Architecture, Prototype-Friendly)
We structure the code so the “backtest logic” is independent of Yahoo Finance, HTTP, DB, etc.

### Layers
**Domain (Enterprise / core):**
- Entities + value objects
- Strategy interface + implementations
- Backtest engine
- Metrics calculation

**Application (Use Cases):**
- Orchestrates: fetch data → run strategy → compute metrics → return DTOs
- UC services: RunBacktest, ListStrategies, ValidateTicker, CompareBaselines

**Interface Adapters:**
- Controllers (REST)
- DTO mapping (API <-> UseCase)
- Presenter/Result formatting

**Infrastructure:**
- Yahoo Finance client (HTTP)
- Cache (in-memory + optional file cache)
- Persistence (optional later)

---

## 5) Domain Model (Minimal)
### Entities / Value Objects
- `Instrument { symbol, name?, assetType? }`
- `Candle { date, open, high, low, close, adjClose?, volume }`
- `TimeSeries { instrument, candles[] }`
- `Portfolio { cash, shares, equity(date) }`
- `Trade { date, side(BUY/SELL), qty, price, reason }`
- `BacktestConfig { startDate, endDate, initialCapital, contribution?, frequency? }`
- `StrategyConfig` (per strategy)
- `BacktestResult { equityCurve[], trades[], metrics, config, strategyId }`

### Interfaces
- `MarketDataProvider`
  - `TimeSeries getDailySeries(symbol, start, end)`
- `Strategy`
  - `StrategyId id()`
  - `BacktestResult run(TimeSeries ts, BacktestConfig cfg, StrategyConfig scfg)`

(You can implement strategies either as “signal generators” + a shared engine, or “each strategy runs its own loop.” For prototype speed, let each strategy run its own loop but share common helpers.)

---

## 6) Data Integration: Yahoo Finance
### Approach
- Use a simple HTTP client to call a Yahoo Finance endpoint that returns chart data (daily).
- Implement:
  - `YahooFinanceMarketDataProvider` in Infrastructure
  - A caching wrapper `CachedMarketDataProvider`

### Caching
- Key: `symbol + startDate + endDate + interval`
- Store in-memory map; optionally dump to `./cache/*.json` on shutdown

### Failure Handling
- If Yahoo blocks/rate-limits:
  - exponential backoff
  - return a clear error DTO
  - keep a fallback “sample dataset” in repo for demo

---

## 7) API (Prototype REST)
(Keep it dead simple. No auth. JSON only.)

### Endpoints
- `GET /api/strategies`
  - returns strategy ids + parameter schema
- `GET /api/instruments/validate?symbol=SPY`
  - returns ok/error + basic instrument info if available
- `POST /api/backtests`
  - body:
    ```json
    {
      "symbol": "AAPL",
      "startDate": "2018-01-01",
      "endDate": "2024-12-31",
      "initialCapital": 10000,
      "strategyId": "ma_crossover",
      "strategyParams": { "shortWindow": 20, "longWindow": 200 },
      "compareTo": ["BUY_HOLD", "SPY_DCA", "SPY_BUY_HOLD"]
    }
    ```
  - returns:
    - equity curve series for each compared run
    - metrics table
    - trade list for the main strategy

### Output Metrics (minimum)
- Final value
- Total contributions (if DCA)
- CAGR
- Max drawdown
- Volatility (annualized from daily returns)
- Sharpe (rf=0)
- Number of trades

---

## 8) Tech Stack (Suggested for Speed)
- Java 25
- Spring Boot 4 (REST + JSON) **for prototype speed**
- Gradle
- JUnit 5
- (Optional) Simple frontend:
  - minimal React/Vite OR plain HTML page calling the API

If you already planned a “prototype in Spring Boot” as a starter template: do it.

---

## 9) Repository Structure
```
/src
/main/java
/com/github/mezink/pm3
/domain
/model
/strategy
/engine
/metrics
/application
/usecase
/dto
/adapters
/web
/presenter
/infrastructure
/marketdata/yahoo
/cache
/test/java
... unit tests ...
/cache (gitignored)
/docs
PLAN.md
```

---

## 10) Implementation Steps (Concrete, Order Matters)
### Step 1 — Skeleton + Domain Contracts (Day 1–2)
- Create modules/packages
- Define:
  - `Candle`, `TimeSeries`, `BacktestConfig`, `BacktestResult`
  - `MarketDataProvider`, `Strategy`
- Add a tiny `BacktestMath` helper (returns, drawdown)

Deliverable: project compiles, tests run.

### Step 2 — Yahoo Finance Provider + Cache (Day 2–4)
- Implement `YahooFinanceMarketDataProvider`
- Parse daily candles into `TimeSeries`
- Add `CachedMarketDataProvider`
- Add integration test with a known ticker (SPY) and a short date range

Deliverable: `getDailySeries("SPY", ...)` returns candles reliably.

### Step 3 — Baselines: Buy & Hold + DCA (Day 4–6)
- Implement S2 Buy & Hold
- Implement S1 DCA
- Compute equity curve and metrics
- Unit tests using a small deterministic candle set

Deliverable: backtests produce stable metrics and curves.

### Step 4 — Signal Strategy (MA Crossover) (Day 6–8)
- Implement SMA calculator
- Implement crossover logic (invested/cash switching)
- Add trade logs + reason strings

Deliverable: MA strategy runs + outputs trades.

### Step 5 — REST API (Day 8–10)
- `/strategies`
- `/instruments/validate`
- `/backtests`
- Add request validation + helpful errors

Deliverable: run a backtest via curl/Postman.

### Step 6 — Comparison Mode (Day 10–12)
- In one request, run:
  - main strategy on symbol
  - baselines (including SPY)
- Return aligned equity curves (same date axis; forward-fill where needed)

Deliverable: comparison result payload.

### Step 7 (Optional) — Mean Reversion Strategy + Simple UI (Week-end buffer)
- Add S4 mean reversion
- Add a minimal UI: select ticker, dates, strategy, run, show chart + metrics

---

## 11) Testing Plan (Prototype but not sloppy)
### Unit Tests (Domain)
- SMA calculation correctness
- DCA: contributions invested at correct intervals
- Buy & Hold: final shares and equity
- Drawdown calculation
- Return series + CAGR sanity checks

### Integration Tests (Infrastructure)
- Yahoo Finance fetch for a small range
- Cache hit/miss behavior

### Golden Data Tests
- Include `src/test/resources/sample_series.csv`
- Run strategies on it and assert exact expected outputs

---

## 12) Risks & Mitigations
- **Yahoo data access unstable / blocked**
  - Mitigation: caching + fallback sample dataset + clear error
- **Time axis alignment across instruments**
  - Mitigation: normalize to trading-day calendar from each series, then align by date keys
- **Strategy ambiguity (execution price / timing)**
  - Mitigation: define: “signals evaluated on close, trades executed on same close” (consistent)

---

## 13) Definition of Done (Prototype)
- Can run a backtest for AAPL with MA crossover and compare to SPY DCA + SPY buy&hold
- Returns equity curves + metrics + trades
- Clean separation: domain does not import Spring or HTTP classes
- Tests exist for metrics and at least two strategies
- One command to run locally (`./gradlew bootRun`)

---

## 14) Next Up After Prototype (If You Continue)
- Multi-asset portfolios
- Fees + slippage models
- Dividend handling (total return)
- Parameter sweeps / optimization grid
- Persist results + run history
- Frontend charting + export to CSV