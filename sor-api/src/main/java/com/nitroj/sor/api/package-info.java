/**
 * Public Adaptive Quantum SOR framework API.
 *
 * <p>Responsibility: expose only the stable engine contract, immutable public
 * DTOs, event types, and opaque handles that integrators compile against.</p>
 *
 * <p>Role in system: this package is consumed by embedded users, future Aeron
 * clients, and the `sor-core` implementation. It must never expose
 * implementation types from `sor-core`.</p>
 *
 * <p>Lifecycle: introduced in Phase 8 as the public framework surface. Later
 * cards add SPI interfaces under `com.nitroj.sor.api.spi` without changing
 * this package's zero-dependency invariant.</p>
 *
 * <p>Design intent: keep the integration contract small, explicit, and stable
 * while allowing engine internals to evolve independently.</p>
 */
package com.nitroj.sor.api;
