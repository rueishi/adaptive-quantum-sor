// Phase 2 tactical optimizer buffer layout.
//
// Byte order: little-endian.
// Input header int32 fields:
//   magic, schemaVersion, instrumentCount, venueCount, regimeCount,
//   urgencyCount, subsetOffsetLength, selectedVenueLength
// Followed by int32 subset offsets and int32 selected venue IDs.
#pragma once

#include <stdint.h>

#define SOR_TACTICAL_MAGIC 0x53524F32
#define SOR_TACTICAL_SCHEMA_VERSION 1

struct SorTacticalInputHeader {
    int32_t magic;
    int32_t schema_version;
    int32_t instrument_count;
    int32_t venue_count;
    int32_t regime_count;
    int32_t urgency_count;
    int32_t subset_offset_length;
    int32_t selected_venue_length;
};
