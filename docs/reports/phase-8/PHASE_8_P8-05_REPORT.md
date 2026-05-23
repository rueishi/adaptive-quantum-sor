# P8-05 Completion Report

## Implemented Scope

P8-05 freezes HotRouteBook ABI v1, including the binary layout document,
reader/writer, CRC protection, golden bytes, and failure handling.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P8-ABI-001 HotRouteBookAbiV1LayoutTest
P8-ABI-002 HotRouteBookAbiV1RoundTripTest
P8-ABI-003 HotRouteBookAbiV1GoldenTest
P8-ABI-004 HotRouteBookAbiV1CrcTest
P8-ABI-005 HotRouteBookAbiV1MagicMismatchTest
P8-ABI-006 HotRouteBookAbiV1VersionMismatchTest
P8-ABI-007 HotRouteBookAbiV1TruncationTest
P8-ABI-008 HotRouteBookAbiV1EndiannessTest
P8-ABI-009 HotRouteBookAbiV1NoSilentChangesTest
P8-ABI-010 HotRouteBookAbiV1ReservedPaddingTest
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
./gradlew :sor-core:test --tests com.nitroj.sor.core.abi.*
```

