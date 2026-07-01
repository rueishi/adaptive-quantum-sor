package com.nitroj.sor.optnative;

import com.nitroj.sor.core.optimizer.TacticalPolicyResult;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Responsibility: define the Phase 2 native tactical optimizer buffer schema.
 *
 * <p>Role in system: Java and C++ agree on this little-endian contiguous layout
 * for tactical optimizer requests and responses.</p>
 *
 * <p>Relationships: {@link TacticalOptimizerNativeInput} writes input buffers,
 * tests use native-probe readers, and future JNI/Panama glue can pass these
 * direct buffers without Java object graphs.</p>
 *
 * <p>Lifecycle: schema version is fixed for Phase 2 MVP. Any incompatible
 * layout change must increment {@link #SCHEMA_VERSION} and reject mismatches.</p>
 *
 * <p>Design intent: keep the ABI explicit, deterministic, and inspectable from
 * Java tests even when CUDA hardware is unavailable.</p>
 */
public final class TacticalOptimizerBufferLayout {
    public static final int MAGIC = 0x53524F32;
    public static final int SCHEMA_VERSION = 1;

    private static final int HEADER_INTS = 8;
    private static final int HEADER_BYTES = HEADER_INTS * Integer.BYTES;

    private TacticalOptimizerBufferLayout() {
    }

    /**
     * Encodes request header, strategic subset offsets, and selected venue IDs.
     *
     * <p>Header layout is: magic, schema, instrumentCount, venueCount,
     * regimeCount, urgencyCount, subsetOffsetLength, selectedVenueLength. All
     * following values are 32-bit little-endian integers. Selected venue IDs are
     * widened from Java shorts for simple C++ reading.</p>
     *
     * @param request validated native optimizer input
     * @return direct little-endian input buffer positioned at zero
     */
    public static ByteBuffer writeInput(final TacticalOptimizerNativeInput request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        final PolicyOptimizationInput input = request.input();
        final int offsetLength = request.subset().subsetOffset.length;
        final int venueLength = request.subset().selectedVenueIds.length;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(HEADER_BYTES + (offsetLength + venueLength) * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(MAGIC);
        buffer.putInt(SCHEMA_VERSION);
        buffer.putInt(input.instrumentCount);
        buffer.putInt(input.venueCount);
        buffer.putInt(input.regimeCount);
        buffer.putInt(input.urgencyCount);
        buffer.putInt(offsetLength);
        buffer.putInt(venueLength);
        for (int offset : request.subset().subsetOffset) {
            buffer.putInt(offset);
        }
        for (short venueId : request.subset().selectedVenueIds) {
            buffer.putInt(venueId);
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Creates a direct output buffer for IVRU tactical arrays.
     *
     * <p>Output layout is: magic, schema, ivruLength, followed by venueWeightBps
     * values. The MVP keeps one array in the buffer probe because remaining
     * arrays are generated deterministically by the Java/CUDA bridge contract.</p>
     *
     * @param ivruLength number of IVRU output cells
     * @return direct little-endian output buffer
     */
    public static ByteBuffer allocateOutput(final int ivruLength) {
        if (ivruLength <= 0) {
            throw new IllegalArgumentException("ivruLength must be positive");
        }
        final ByteBuffer buffer = ByteBuffer.allocateDirect(3 * Integer.BYTES + ivruLength * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(MAGIC);
        buffer.putInt(SCHEMA_VERSION);
        buffer.putInt(ivruLength);
        for (int i = 0; i < ivruLength; i++) {
            buffer.putInt(0);
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Writes a tactical weight array into an output buffer.
     *
     * @param buffer output buffer created by {@link #allocateOutput(int)}
     * @param weights IVRU venue weight values
     */
    public static void writeOutputWeights(final ByteBuffer buffer, final int[] weights) {
        validateOutputBuffer(buffer);
        if (weights == null || weights.length != buffer.getInt(2 * Integer.BYTES)) {
            throw new IllegalArgumentException("weights length must match output ivruLength");
        }
        final ByteBuffer duplicate = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        duplicate.position(3 * Integer.BYTES);
        for (int weight : weights) {
            duplicate.putInt(weight);
        }
    }

    /**
     * Reads output weights into a tactical result shell.
     *
     * @param buffer output buffer
     * @return tactical result with venueWeightBps populated
     */
    public static TacticalPolicyResult readOutputWeights(final ByteBuffer buffer) {
        validateOutputBuffer(buffer);
        final ByteBuffer duplicate = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        final int length = duplicate.getInt(2 * Integer.BYTES);
        duplicate.position(3 * Integer.BYTES);
        final TacticalPolicyResult result = new TacticalPolicyResult();
        result.venueWeightBps = new int[length];
        for (int i = 0; i < length; i++) {
            result.venueWeightBps[i] = duplicate.getInt();
        }
        return result;
    }

    /**
     * Reads a header integer for C++-style probe tests.
     *
     * @param buffer input buffer
     * @param field zero-based header field
     * @return integer value at the requested header field
     */
    public static int readHeaderField(final ByteBuffer buffer, final int field) {
        validateInputBuffer(buffer);
        if (field < 0 || field >= HEADER_INTS) {
            throw new IndexOutOfBoundsException("header field out of range: " + field);
        }
        return buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).getInt(field * Integer.BYTES);
    }

    /**
     * Reads a strategic subset offset from an input buffer.
     *
     * @param buffer input buffer
     * @param index subset offset index
     * @return offset value
     */
    public static int readSubsetOffset(final ByteBuffer buffer, final int index) {
        validateInputBuffer(buffer);
        final int offsetLength = readHeaderField(buffer, 6);
        if (index < 0 || index >= offsetLength) {
            throw new IndexOutOfBoundsException("subset offset index out of range: " + index);
        }
        return buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).getInt(HEADER_BYTES + index * Integer.BYTES);
    }

    /**
     * Reads a selected venue ID from an input buffer.
     *
     * @param buffer input buffer
     * @param index selected venue index
     * @return venue ID
     */
    public static int readSelectedVenueId(final ByteBuffer buffer, final int index) {
        validateInputBuffer(buffer);
        final int offsetLength = readHeaderField(buffer, 6);
        final int venueLength = readHeaderField(buffer, 7);
        if (index < 0 || index >= venueLength) {
            throw new IndexOutOfBoundsException("selected venue index out of range: " + index);
        }
        return buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                .getInt(HEADER_BYTES + (offsetLength + index) * Integer.BYTES);
    }

    /**
     * Ensures an input buffer uses the expected magic and schema version.
     *
     * @param buffer native input buffer
     */
    public static void validateInputBuffer(final ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        final ByteBuffer duplicate = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (duplicate.capacity() < HEADER_BYTES || duplicate.getInt(0) != MAGIC) {
            throw new IllegalArgumentException("native input magic mismatch");
        }
        if (duplicate.getInt(Integer.BYTES) != SCHEMA_VERSION) {
            throw new IllegalArgumentException("native input schema mismatch");
        }
    }

    private static void validateOutputBuffer(final ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        final ByteBuffer duplicate = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (duplicate.capacity() < 3 * Integer.BYTES || duplicate.getInt(0) != MAGIC) {
            throw new IllegalArgumentException("native output magic mismatch");
        }
        if (duplicate.getInt(Integer.BYTES) != SCHEMA_VERSION) {
            throw new IllegalArgumentException("native output schema mismatch");
        }
    }
}
