package com.matyrobbrt.codecutils.test

import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat
import static org.assertj.core.api.Assertions.assertThat as assertOptional

@POJO
@CompileStatic
class RecordSerializationTest {
    static final CodecCreator CREATOR = CodecCreator.create()
    static final Codec<TestRecord> CODEC = CREATOR.getCodec(TestRecord)

    @Test
    void "record can be serialized"() {
        final record = new TestRecord('Hello World!', 725, ['Not an empty list!'])
        final serialized = CODEC.encodeStart(ObjectOps.INSTANCE, record)
        assertThat(serialized)
                .isPresent()
                .hasValue([
                        someValue      : 'Hello World!',
                        anotherValue   : 725,
                        yetAnotherValue: ['Not an empty list!']
                ])
    }

    @Test
    void "record can be deserialized"() {
        final serialized = CODEC.parse(ObjectOps.INSTANCE, [
                someValue      : 'A string value',
                anotherValue   : 9,
                yetAnotherValue: ['list entry 1', 'list entry 2']
        ])

        assertThat(serialized)
                .isPresent()
                .hasValue(new TestRecord(
                        'A string value', 9, ['list entry 1', 'list entry 2']
                ))
    }

    @Test
    void "record can go both ways"() {
        final record = new TestRecord('I can go both ways!', Integer.MAX_VALUE - 167483, listFilledWith(264, 'Dummy Entry'))

        final serialized = CODEC.encodeStart(ObjectOps.INSTANCE, record)
        assertThat(serialized).isPresent()

        final deserialized = CODEC.parse(ObjectOps.INSTANCE, serialized.result().orElseThrow())
        assertThat(deserialized).hasValue(record)
    }

    @Test
    void "cannot deserialize with missing values"() {
        final deserialized = CODEC.parse(ObjectOps.INSTANCE, [
                someValue: 'Only a string is here!'
        ])

        assertOptional(deserialized.error()).isPresent()
    }

    @POJO
    static record TestRecord(
            String someValue, int anotherValue, List<String> yetAnotherValue
    ) {}

    private static <T> List<T> listFilledWith(int size, T value) {
        final list = new ArrayList<T>(size)
        for (int i in 0..size) list.add(value)
        return list
    }
}