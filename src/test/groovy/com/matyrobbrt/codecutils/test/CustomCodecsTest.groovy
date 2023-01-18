//file:noinspection UnnecessaryQualifiedReference
package com.matyrobbrt.codecutils.test

import com.google.gson.reflect.TypeToken
import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.datafixers.util.Either
import com.mojang.datafixers.util.Pair
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

    static final Codec<Either<String, Integer>> EITHER = CREATOR.getCodec(new TypeToken<Either<String, Integer>>() {})
    static final Codec<Pair<String, Map<String, Float>>> PAIR = CREATOR.getCodec(new TypeToken<Pair<String, Map<String, Float>>>() {})
    static final Codec<Set<String>> STRING_SET = CREATOR.getCodec(new TypeToken<Set<String>>() {})
    static final Codec<Vector<Integer>> INT_VECTOR = CREATOR.getCodec(new TypeToken<Vector<Integer>>() {})
    static final Codec<Stack<Character>> CHAR_STACK = CREATOR.getCodec(new TypeToken<Stack<Character>>() {})
    static final Codec<Queue<String>> STRING_QUEUE = CREATOR.getCodec(new TypeToken<Queue<String>>() {})

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

    @Test
    void "Either can be deserialized"() {
        assertThat(EITHER.parse(ObjectOps.INSTANCE, 12)).hasValue(Either.right(12))
    }

    @Test
    void "Either can be serialized"() {
        assertThat(EITHER.encodeStart(ObjectOps.INSTANCE, Either.left('A String'))).hasValue('A String')
    }

    @Test
    void "Pair can be deserialized"() {
        assertThat(PAIR.parse(ObjectOps.INSTANCE, [
                'first': 'Some String',
                'second': ['A key': 14f]
        ])).hasValue(Pair.of('Some String', ['A key': 14f]) as Pair)
    }

    @Test
    void "Pair can be serialized"() {
        CustomAssertions.<Object>assertThat(PAIR.encodeStart(ObjectOps.INSTANCE, Pair.of(
                'String ftw',
                ['Key1': 034f] as Map
        )))
                .hasValue([
                        'first': 'String ftw',
                        'second': ['Key1': 034f]
                ])
    }

    @Test
    void "Set can be deserialized"() {
        // Sets should be unordered
        assertThat(STRING_SET.parse(ObjectOps.INSTANCE, ['String1', 'String 2'])).hasValue(['String 2', 'String1'] as Set)
    }

    @Test
    void "Set can be serialized"() {
        assertThat(STRING_SET.encodeStart(ObjectOps.INSTANCE, ['String val 1'] as Set)).hasValue(['String val 1'])
    }

    @Test
    void "Queue can be deserialized"() {
        assertThat(STRING_QUEUE.parse(ObjectOps.INSTANCE, ['String1', 'String 2'])).hasValue(['String1', 'String 2'] as Queue)
    }

    @Test
    void "Queue can be serialized"() {
        assertThat(STRING_QUEUE.encodeStart(ObjectOps.INSTANCE, ['String val 1'] as Queue)).hasValue(['String val 1'])
    }

    @Test
    void "Vector can be deserialized"() {
        assertThat(INT_VECTOR.parse(ObjectOps.INSTANCE, [24545, 143]))
                .hasValue(new Vector<Integer>([24545, 143]))
    }

    @Test
    void "Vector can be serialized"() {
        assertThat(INT_VECTOR.encodeStart(ObjectOps.INSTANCE, new Vector<Integer>((14..500).collect())))
                .hasValue((14..500).collect())
    }

    @Test
    void "Stack can be deserialized"() {
        assertThat(CHAR_STACK.parse(ObjectOps.INSTANCE, ['a', 'b', 'c', 'd']))
                .hasValue(new Stack<Character>().tap {
                    push('a' as char)
                    push('b' as char)
                    push('c' as char)
                    push('d' as char)
                })
    }

    @Test
    void "Stack can be serialized"() {
        assertThat(CHAR_STACK.encodeStart(ObjectOps.INSTANCE, new Stack<Character>().tap {
            push('x' as char)
            push('y' as char)
            push('z' as char)
        }))
                .hasValue(['x', 'y', 'z'])
    }
}
