# PLAN.md — Backtesting Platform Prototype (Yahoo Finance + Hardcoded Strategies)

## 0) Goal (Prototype Scope)

Build a small backtesting platform that lets a user:

1) Select one or more instruments (tickers) sourced from Yahoo Finance
2) Pick a strategy (hardcoded for now) and parameters (start date, capital, frequency, etc.)
3) Pick another strategy or baseline (e.g., buy & hold, SPY DCA) to compare against
4) Run the backtest and get:

- Equity curves for each strategy (time series of portfolio value)
- Key metrics (final value, CAGR, drawdown, etc.)
- Trade list for the main strategy (date, action, qty, price, reason)

Prototype-first: correctness, clarity, and a clean internal architecture matter more than feature breadth. We will keep
data frequency to **daily candles** and ignore intraday, corporate actions edge cases, and advanced order types.

(We follow the “build a working prototype in iterative increments + clean architecture / maintainable code” mindset from
the course kickoff, but we’re not blocking ourselves with “no frameworks” rules.) :contentReference[oaicite:0]{index=0}


---

## 1) Core Use Cases

### UC1 — SelectInstrument

- User searches/inputs ticker(s): e.g., AAPL, MSFT, BTC-USD, SPY
- System validates ticker (exists / fetchable)
- System fetches and caches historical price series

### UC2 - ListStrategies

- User requests available strategies
- We must find a way for the UI to know what parameters each strategy needs (e.g., DCA needs contributionAmount +
  frequency, MA crossover needs shortWindow + longWindow)

### UC3 — RunBacktest

- Runs chosen strategy(ies) on chosen instrument(s)
- User selects a strategy and config:
    - start/end date
    - initial capital
    - slipage, fees
    - strategy parameters (reoccurring contribution, window length, threshold, etc.)
- Runs baselines on same timeframe
- Produces comparison metrics + equity curves

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
- UC services: Our UseCases

**Interface Adapters:**

- Controllers (REST)
- DTO mapping (API <-> UseCase)
- Presenter/Result formatting

**Infrastructure:**

- Yahoo Finance client (HTTP)
- Cache (in-memory)

---

## 5) Domain Model (Minimal)

### Entities / Value Objects

Think of these on the fly as you implement, but here are some starting points:

- `Instrument` symbol, name, assetType
- `Candle`: date, open, high, low, close, volume
- `TimeSeries`: `Instrument`, list of `Candle`s

### Interfaces

Think of these on the fly as you implement, but here are some starting points:

- `MarketDataProvider`
    - `TimeSeries getDailySeries(instrument, start, end)`

Strategies MUST use the strategy pattern. Define a `Strategy` interface. Each strategy implementation (DCA, MA
crossover) implements this interface.

---

## 6) Data Integration: Yahoo Finance

### Approach

- Use a simple HTTP client to call a Yahoo Finance endpoint that returns chart data (daily).
- Implement:
    - `YahooFinanceMarketDataProvider` in Infrastructure
    - A caching wrapper `CachedMarketDataProvider`

### Caching

- Key: `symbol + startDate + endDate + interval`

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
    - think of the return type on the fly, but at the minimum, it should include the stategy id
- `GET /api/instruments/validate?symbol=SPY`
    - returns ok/error + basic instrument info if available
- `POST /api/backtest`
    - body:
      ```json
      {
      "backtests": [
        {
          "symbol": "AAPL",
          "startDate": "2018-01-01",
          "endDate": "2024-12-31",
          "initialCapital": 10000,
          "strategyId": "ma_crossover",
          ...
        },
        {
          "symbol": "SPY",
          "startDate": "2018-01-01",
          "endDate": "2024-12-31",
          "initialCapital": 10000,
          "strategyId": "dca",
          ...
        }
      ]
      }
      
      
      ```
    - returns:
        - equity curve series for each run
        - metrics table
        - trade list for the strategies that produce trades (e.g., MA crossover)

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
- Gradle (use the sonarlint and pmd plugin for code quality checks)
- JUnit 5
- (Optional) Simple frontend:
    - minimal HTML page

---

## 9) Repository Structure

Follow the principles of clean architecture and standard Gradle project layout:

This means you have at minimum domain, application, and infrastructure packages, and the domain package does not depend
on Spring or HTTP classes.


---

## 10) Implementation Steps (Concrete, Order Matters)

### Step 1 — Skeleton + Domain Contracts (Day 1–2)

- Create modules/packages
- Define:
    - `Candle`, `TimeSeries`, etc. 
    - Create a helper for the Backtest maths
Deliverable: project compiles, tests run.

### Step 2 — Yahoo Finance Provider + Cache (Day 2–4)

- Implement `YahooFinanceMarketDataProvider`
- Parse daily candles into `TimeSeries`
- Add `InMemoryCachedMarketDataProvider`
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
- `/backtest`

Deliverable: run a backtest via curl/Postman.

### Step 6 — Comparison Mode (Day 10–12)

- In one request, run:
  - add multiple backtest configs (e.g., AAPL MA crossover + MSFT DCA + SPY Buy & Hold)
- Return aligned equity curves (same date axis; forward-fill where needed)

Deliverable: comparison result payload.

### Step 7 (Optional) — Mean Reversion Strategy + Simple UI (Week-end buffer)

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