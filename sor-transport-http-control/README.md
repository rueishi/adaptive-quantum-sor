# SOR HTTP Control Plane

This module is the built-in HTTP control plane for users who embed Adaptive
Quantum SOR and want a ready-made operational surface.

It is research and ops only and is not the production ultra-low-latency order
intake path. Production order flow should use the embedded Java API, Aeron, or
an integrator-owned gateway. This server exposes liveness, readiness,
Prometheus metrics, order/status inspection, policy identity, and typed
control-plane diagnostics over HTTP.

## Supported User Endpoints

```text
GET  /healthz
GET  /ready
GET  /metrics
GET  /openapi.json
POST /orders
GET  /orders/{id}
GET  /policy/current
GET  /control/state
GET  /control/market-data
POST /control/reset
```

These endpoints are backed by `SorEngine`, `SorControlPlane`, and
`Observability`. The module intentionally does not depend on `sor-test-server`
or simulator packages.

## Not Included

Scenario/demo endpoints stay in `sor-test-server`:

```text
POST /scenario/reset
POST /scenario/run
GET  /scenario/summary
GET  /scenario/events
GET  /events/stream
```

Those endpoints are for notebook demos, simulator scenario replay, and test
fixtures. They are not part of the built-in user HTTP control module.

## Embedded Usage

```java
SorEngine engine = SorEngineBuilder.create()
        .config(config)
        .marketData(marketDataSource)
        .venueAdapter(venueAdapter)
        .riskProvider(riskProvider)
        .persistence(persistence)
        .observability(observability)
        .build();

HttpControlPlaneServer http = new HttpControlPlaneServer(8080, engine, observability);
http.start();
```

If the engine also implements `SorControlPlane`, the `/control/*` endpoints are
available. Otherwise those endpoints return `501`.
