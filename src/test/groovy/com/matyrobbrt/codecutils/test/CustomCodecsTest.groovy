package com.matyrobbrt.codecutils.test

import com.google.gson.reflect.TypeToken
import com.matyrobbrt.codecutils.CodecCreator
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat

@POJO
@CompileStatic
class CustomCodecsTest {
    static final CodecCreator CREATOR = CodecCreator.create()

    static final Codec<UUID> UUID_CODEC = CREATOR.getCodec(UUID)

    static final Codec<Map<List<String>, String>> NON_STRING_KEY = CREATOR.getCodec(new TypeToken<Map<List<String>, String>>() {})
    static final Codec<Map<Integer, String>> STRING_KEY = CREATOR.getCodec(new TypeToken<Map<Integer, String>>() {})

    @Test
    void "UUID can be serialized"() {
        final uid = UUID.randomUUID()
        assertThat(UUID_CODEC.encodeStart(ObjectOps.INSTANCE, uid))
            .hasValue(uid.toString())
    }

    @Test
    void "UUID can be deserialized"() {
        final uid = UUID.randomUUID()
        assertThat(UUID_CODEC.parse(ObjectOps.INSTANCE, uid.toString()))
            .hasValue(uid)
    }

    @Test
    void "Map with non-string key can be deserialized"() {
        assertThat(NON_STRING_KEY.parse(ObjectOps.INSTANCE, [
                ['key': ['a', 'b'], 'value': 'A string']
        ]))
            .hasValue([
                    ['a', 'b']: 'A string'
            ])
    }

    @Test
    void "Map with non-string key can be serialized"() {
        assertThat(NON_STRING_KEY.encodeStart(ObjectOps.INSTANCE, [
                ['a', 'b']: 'A string'
        ]))
            .hasValue([
                    ['key': ['a', 'b'], 'value': 'A string']
            ])
    }

    @Test
    void "Map with string-like key can be deserialized"() {
        assertThat(STRING_KEY.parse(ObjectOps.INSTANCE, ['24': 'A string'])).hasValue([24: 'A string'])
    }

    @Test
    void "Map with string-like key can be serialized"() {
        assertThat(STRING_KEY.encodeStart(ObjectOps.INSTANCE, [24: 'A string'])).hasValue(['24': 'A string'])
    }
}
