package com.matyrobbrt.codecutils.test.annotation

import com.matyrobbrt.codecutils.CodecCreator
import com.matyrobbrt.codecutils.annotation.SingleOrList
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat

@POJO
@CompileStatic
class SingleOrListTest {
    static final CodecCreator CREATOR = CodecCreator.create()
    static final Codec<WithSingleOrList> CODEC = CREATOR.getCodec(WithSingleOrList)

    @Test
    void "@SingleOrList with one value serializes to single"() {
        assertThat(CODEC.encodeStart(ObjectOps.INSTANCE, new WithSingleOrList([14])))
                .hasValue([values: 14])
    }

    @Test
    void "@SingleOrList with multiple value serializes to list"() {
        assertThat(CODEC.encodeStart(ObjectOps.INSTANCE, new WithSingleOrList([14, 15, 16])))
                .hasValue([values: [14, 15, 16]])
    }

    @Test
    void "@SingleOrList can deserialize single value"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [values: 14]))
                .hasValue(new WithSingleOrList([14]))
    }

    @Test
    void "@SingleOrList can deserialize list value"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [values: [14, 16, 18]]))
                .hasValue(new WithSingleOrList([14, 16, 18]))
    }

    static record WithSingleOrList(@SingleOrList List<Integer> values) {}
}
