package com.matyrobbrt.codecutils.test.annotation

import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.api.annotation.OrEmpty
import com.matyrobbrt.codecutils.api.annotation.SingleOrList
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat

@POJO
@CompileStatic
class OrEmptyTest {
    static final CodecCreator CREATOR = CodecCreator.create()
    static final Codec<WithOrEmpty> CODEC = CREATOR.getCodec(WithOrEmpty)
    static final Codec<WithOrEmptyAndSingleOrList> CODEC_SOR = CREATOR.getCodec(WithOrEmptyAndSingleOrList)

    @Test
    void "@OrEmpty can deserialize missing value"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [:]))
            .hasValue(new WithOrEmpty([]))
    }

    @Test
    void "@OrEmpty can deserialize value"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [values: [1d, 2d, 3d, 4d]]))
            .hasValue(new WithOrEmpty([1d, 2d, 3d, 4d]))
    }


    @Test
    void "@OrEmpty & @SingleOrList can deserialize missing value"() {
        assertThat(CODEC_SOR.parse(ObjectOps.INSTANCE, [:]))
                .hasValue(new WithOrEmptyAndSingleOrList([]))
    }

    @Test
    void "@OrEmpty & @SingleOrList can deserialize single value"() {
        assertThat(CODEC_SOR.parse(ObjectOps.INSTANCE, [values: 'Hello there!']))
            .hasValue(new WithOrEmptyAndSingleOrList(['Hello there!']))
    }

    @Test
    void "@OrEmpty & @SingleOrList can deserialize multiple values"() {
        assertThat(CODEC_SOR.parse(ObjectOps.INSTANCE, [values: ['Hello there!', 'How are you?']]))
            .hasValue(new WithOrEmptyAndSingleOrList(['Hello there!', 'How are you?']))
    }

    static record WithOrEmpty(@OrEmpty List<Double> values) {}
    static record WithOrEmptyAndSingleOrList(@OrEmpty @SingleOrList List<String> values) {}
}
