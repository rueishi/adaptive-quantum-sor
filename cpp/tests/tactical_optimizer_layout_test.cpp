// Unit tests for the Phase 2 tactical optimizer C buffer layout.
#include "../tactical_optimizer_layout.h"

#include <cassert>
#include <cstdint>

int main() {
    SorTacticalInputHeader header{};
    header.magic = SOR_TACTICAL_MAGIC;
    header.schema_version = SOR_TACTICAL_SCHEMA_VERSION;
    header.instrument_count = 2;
    header.venue_count = 3;
    header.regime_count = 4;
    header.urgency_count = 5;
    header.subset_offset_length = 2 * 4 * 5 + 1;
    header.selected_venue_length = 6;

    assert(header.magic == 0x53524F32);
    assert(header.schema_version == 1);
    assert(header.subset_offset_length == 41);
    assert(header.selected_venue_length == 6);
    return 0;
}
