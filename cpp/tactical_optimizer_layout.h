/**
 * @file
 * @brief Packed tactical optimizer buffer layout shared by C++ and Java.
 *
 * All integer fields are little-endian int32 values. Callers own the byte
 * buffers and must validate lengths before reading variable sections. The
 * fixed header is followed by int32 subset offsets and int32 selected venue
 * IDs.
 */

#pragma once

#include <stdint.h>

#define SOR_TACTICAL_MAGIC 0x53524F32
#define SOR_TACTICAL_SCHEMA_VERSION 1

/**
 * @brief Fixed prefix for a tactical optimizer input buffer.
 */
struct SorTacticalInputHeader {
    ///< Magic marker SOR_TACTICAL_MAGIC. Reject buffers with any other value.
    int32_t magic;

    ///< Schema version for this fixed header and trailing int32 array layout.
    int32_t schema_version;

    ///< Instrument dimension used to size policy/regime/urgency routing data.
    int32_t instrument_count;
    ///< Venue dimension used to size policy/regime/urgency routing data.
    int32_t venue_count;
    ///< Regime dimension used to size policy/regime/urgency routing data.
    int32_t regime_count;
    ///< Urgency dimension used to size policy/regime/urgency routing data.
    int32_t urgency_count;

    ///< Number of int32 entries in the trailing subset-offset array.
    int32_t subset_offset_length;

    ///< Number of int32 entries in the trailing selected-venue array.
    int32_t selected_venue_length;
};
