package com.matyrobbrt.codecutils.test.annotation

import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.api.annotation.DefaultValue
import com.matyrobbrt.codecutils.api.annotation.ValidateWith
import com.matyrobbrt.codecutils.api.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat

@POJO
@CompileStatic
class ValidateWithTest {
    static final Codec<WithValidatableField> CODEC = CodecCreator.create().getCodec(WithValidatableField)
    static final Codec<WithValidatableFieldNoSer> CODEC_NS = CodecCreator.create().getCodec(WithValidatableFieldNoSer)

    @Test
    void "@ValidateWith errors invalid values"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [value: 'Some string']))
                .isEmpty()
    }

    @Test
    void "@ValidateWith returns valid values"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [value: 'Hello yes']))
                .hasValue(new WithValidatableField('Hello yes'))
    }

    @Test
    void "@ValidateWith(whenSerializing = false) does not error when serializing"() {
        assertThat(CODEC_NS.encodeStart(ObjectOps.INSTANCE, new WithValidatableFieldNoSer('not contain')))
            .hasValue(['value': 'not contain'])
    }

    @Test
    void "@ValidateWith(whenSerializing = false) errors when deserializing"() {
        assertThat(CODEC_NS.parse(ObjectOps.INSTANCE, ['value': 'not contain']))
            .isEmpty()
    }

    static record WithValidatableField(@ValidateWith(Validator) String value) {}
    static record WithValidatableFieldNoSer(@ValidateWith(value = Validator, whenSerializing = false) String value) {}

    static final class Validator implements ValidateWith.Validator<String> {

        @Override
        boolean test(String value) {
            return value.contains('yes')
        }

        @Override
        String getMessage(String value) {
            return "\"$value\" does not contain \"yes\""
        }
    }
}