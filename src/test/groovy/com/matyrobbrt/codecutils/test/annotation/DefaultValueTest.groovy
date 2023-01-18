package com.matyrobbrt.codecutils.test.annotation

import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.api.annotation.DefaultValue
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat

@POJO
@CompileStatic
class DefaultValueTest {
    static final Codec<DefValueString> STR_CODEC = CodecCreator.create().getCodec(DefValueString)

    @Test
    void "@DefaultValue can be deserialized"() {
        assertThat(STR_CODEC.parse(ObjectOps.INSTANCE, [value: 'Some string']))
                .hasValue(new DefValueString('Some string'))
    }

    @Test
    void "@DefaultValue can be deserialized without value"() {
        assertThat(STR_CODEC.parse(ObjectOps.INSTANCE, [:]))
                .hasValue(new DefValueString('1290'))
    }

    static record DefValueString(@DefaultValue(stringValue = '1290') String value) {}
}
