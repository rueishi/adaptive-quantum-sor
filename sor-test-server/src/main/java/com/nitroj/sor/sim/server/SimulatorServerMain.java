package com.nitroj.sor.sim.server;

/** Compatibility main class that delegates to the simulator sample server CLI. */
public final class SimulatorServerMain {
    private SimulatorServerMain() {
    }

    public static void main(final String[] args) throws Exception {
        SimulatorServerApplication.main(args);
    }
}
