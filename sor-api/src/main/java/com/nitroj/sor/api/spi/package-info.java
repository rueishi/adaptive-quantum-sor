/**
 * Service-provider interfaces used to integrate Adaptive Quantum SOR with
 * external market data, venue connectivity, risk, persistence, and clocks.
 *
 * <p>Responsibility: define the owner boundary between the framework engine
 * and integrator-supplied state.</p>
 *
 * <p>Role in system: `sor-core` consumes these interfaces, while simulators and
 * real adapters implement them.</p>
 *
 * <p>Lifecycle: introduced in P8-04 after the public engine contract. Later
 * cards implement simulator and production adapters against this package.</p>
 *
 * <p>Design intent: keep hot-path interfaces primitive, reusable, and explicit
 * about allocation and blocking rules.</p>
 */
package com.nitroj.sor.api.spi;
