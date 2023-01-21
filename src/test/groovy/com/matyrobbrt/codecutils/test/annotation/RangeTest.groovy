package com.matyrobbrt.codecutils.test.annotation

import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.api.annotation.Range
import com.matyrobbrt.codecutils.api.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat
import static org.assertj.core.api.Assertions.assertThat as assertOptional

@POJO
@CompileStatic
class RangeTest {
    static final CodecCreator CREATOR = CodecCreator.create()
    static final Codec<RangedInt> RANGED_INT = CREATOR.getCodec(RangedInt)
    static final Codec<RangedFloat> RANGED_FLOAT = CREATOR.getCodec(RangedFloat)

    @Test
    void "@Range int in range can be deserialized"() {
        assertThat(RANGED_INT.parse(ObjectOps.INSTANCE, [value: 44]))
                .hasValue(new RangedInt(44))
    }

    @Test
    void "@Range int not in range cannot be deserialized"() {
        assertOptional(RANGED_INT.parse(ObjectOps.INSTANCE, [value: 220]).error())
                .isPresent()
    }

    @Test
    void "@Range int in range can be serialized"() {
        assertThat(RANGED_INT.encodeStart(ObjectOps.INSTANCE, new RangedInt(153)))
                .hasValue([value: 153])
    }

    @Test
    void "@Range int not in range cannot be serialized"() {
        assertOptional(RANGED_INT.encodeStart(ObjectOps.INSTANCE, new RangedInt(36783)).error())
                .isPresent()
    }

    @Test
    void "@Range float in range can be deserialized"() {
        assertThat(RANGED_FLOAT.parse(ObjectOps.INSTANCE, [value: 44.5f]))
                .hasValue(new RangedFloat(44.5f))
    }

    @Test
    void "@Range float not in range cannot be deserialized"() {
        assertOptional(RANGED_FLOAT.parse(ObjectOps.INSTANCE, [value: 199.91f]).error())
                .isPresent()
    }

    @Test
    void "@Range float in range can be serialized"() {
        assertThat(RANGED_FLOAT.encodeStart(ObjectOps.INSTANCE, new RangedFloat(50.4f)))
                .hasValue([value: 50.4f])
    }

    @Test
    void "@Range float not in range cannot be serialized"() {
        assertOptional(RANGED_FLOAT.encodeStart(ObjectOps.INSTANCE, new RangedFloat(367.245f)).error())
                .isPresent()
    }

    static record RangedInt(@Range(intMin = 14, intMax = 200) int value) {}

    static record RangedFloat(@Range(floatMin = 44.5f, floatMax = 199.9f) float value) {}
}
