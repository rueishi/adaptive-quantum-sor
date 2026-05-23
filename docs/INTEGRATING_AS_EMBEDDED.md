# Integrating As Embedded

Add `com.nitroj.sor:sor-api` and the desired adapter modules to your build,
then construct `SorEngineBuilder` with your market data, venue, risk,
persistence, clock, and observability implementations. Call `warmup(...)`
before production order flow, register listeners for acknowledgements and route
events, and close the engine during process shutdown.

Troubleshooting checklist:
- `/ready` remains false: confirm `warmup(...)` completed.
- Orders reject immediately: inspect risk-provider limits and venue state.
- Metrics missing: wire `SorObservability` or another `Observability`
  implementation before `build()`.
