package com.nitroj.sor.testserver;

/**
 * Responsibility: provide a stable compatibility main class for launching the
 * simulator-backed sample server.
 *
 * <p>Role in system: delegates directly to {@link SimulatorServerApplication}
 * so scripts, packaging, or older launch commands can keep a simple main-class
 * entrypoint while the real application assembly remains in one place.</p>
 *
 * <p>Relationships: has no dependencies beyond the application class and does
 * not own server lifecycle state.</p>
 *
 * <p>Lifecycle: invoked by Java launchers and exits when
 * {@link SimulatorServerApplication#main(String[])} exits.</p>
 *
 * <p>Design intent: preserve launch compatibility without duplicating startup
 * logic or configuration parsing.</p>
 */
public final class SimulatorServerMain {
    private SimulatorServerMain() {
    }

    public static void main(final String[] args) throws Exception {
        SimulatorServerApplication.main(args);
    }
}
