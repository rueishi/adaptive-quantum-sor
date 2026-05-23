# P8-22 Completion Report

## Implemented Scope

P8-22 adds the simulator sample server inside `sor-test-server`, CLI flags, Jib OCI image
configuration, Helm chart, probes, Prometheus scrape path, NetworkPolicy, and
integrator onboarding evidence.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P8-DEPLOY-001 JibImageBuildTest
P8-DEPLOY-002 ContainerProbesSmokeTest
P8-DEPLOY-003 HelmChartLintTest
P8-DEPLOY-004 PrometheusScrapeIntegrationTest
P8-DEPLOY-005 NetworkPolicyEnforcementTest
P8-DEPLOY-006 docs/reports/phase-8/PHASE_8_ONBOARDING_REPORT.md, docs/integration/INTEGRATING_AS_EMBEDDED.md, docs/integration/INTEGRATING_OVER_AERON.md
P8-DEPLOY-007 CliFlagsTest
```

Planned ACs:

```text
none
```

Failed ACs:

```text
none
```

## Validation Commands

```bash
./gradlew :sor-test-server:test
```
