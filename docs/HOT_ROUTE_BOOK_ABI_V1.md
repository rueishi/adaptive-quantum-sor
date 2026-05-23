# HotRouteBook ABI v1

This document freezes the Phase 8 binary layout for `HotRouteBook` snapshots.
The ABI is little-endian and intended for Java recovery, Chronicle persistence,
and the future C++ L0 reader.

## Header

| Offset | Size | Field | Notes |
|---:|---:|---|---|
| 0 | 8 | magic | ASCII `QSORHRB1`, value `0x5152534f48524231` |
| 8 | 4 | version | ABI version, always `1` |
| 12 | 4 | headerFlags | reserved, must be `0` |
| 16 | 4 | instrumentCount | dense instrument count |
| 20 | 4 | venueCount | derived from route venue IDs |
| 24 | 4 | regimeCount | dense regime count |
| 28 | 4 | urgencyCount | dense urgency count |
| 32 | 8 | policyVersion | policy metadata, zero when unavailable |
| 40 | 8 | policyHash64 | policy metadata, zero when unavailable |
| 48 | 8 | effectiveFromEpochNanos | policy metadata, zero when unavailable |
| 56 | 8 | routeEntryCount | total entries in flattened route arrays |
| 64 | 64 | reserved | all zeros in v1; reserved for v2 header growth |

The first payload byte is at offset `128`.

## Payload

For `N = instrumentCount * regimeCount * urgencyCount` and
`M = routeEntryCount`:

| Segment | Element Size | Count |
|---|---:|---:|
| routeStart | 4 | N |
| routeEnd | 4 | N |
| routeVenueId | 4 | M |
| maxChildQty | 8 | M |
| venueWeightBps | 4 | M |
| participationCapBps | 4 | M |
| CRC-32C | 4 | 1 |

The CRC-32C covers every byte from offset `0` through the byte immediately
before the CRC field.

## Versioning Rules

Version 1 offsets, sizes, magic, and semantics must not change. Any
incompatible change requires a new ABI version and a new golden binary.
Version 2 may use the reserved header bytes or append fields before the CRC,
but v1 readers must continue rejecting non-v1 buffers explicitly.
