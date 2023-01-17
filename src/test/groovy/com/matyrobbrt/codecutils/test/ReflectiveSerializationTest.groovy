package com.matyrobbrt.codecutils.test

import com.matyrobbrt.codecutils.CodecCreator
import com.matyrobbrt.codecutils.annotation.*
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static com.matyrobbrt.codecutils.test.CustomAssertions.assertThat
import static org.assertj.core.api.Assertions.assertThat as assertOptional

@POJO
@CompileStatic
class ReflectiveSerializationTest {
    static final CodecCreator CREATOR = CodecCreator.create()
    static final Codec<TestObject> CODEC = CREATOR.getCodec(TestObject)

    @Test
    void "reflective codec can serialize object"() {
        final object = new TestObject('Some string', 12, ['Another string', 'Str2'])
        assertThat(CODEC.encodeStart(ObjectOps.INSTANCE, object))
            .isPresent()
            .hasValue([
                    strValue: 'Some string',
                    intValue: 12,
                    listValue: ['Another string', 'Str2']
            ])
    }

    @Test
    void "reflective codec can deserialize object"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [
                strValue: 'A string',
                intValue: 13,
                listValue: 'ABCDEFG'
        ])).hasValue(new TestObject('A string', 13, ['ABCDEFG']))
    }

    @Test
    void "reflective codec cannot deserialize object with missing values"() {
        assertOptional(CODEC.parse(ObjectOps.INSTANCE, [
                strValue: 'A string',
                listValue: 'ABCDEFG'
        ]).error()).isPresent()
    }

    @Test
    void "reflective codec can deserialize object with missing default values"() {
        assertThat(CODEC.parse(ObjectOps.INSTANCE, [
                strValue: 'A String',
                intValue: 1
        ])).hasValue(new TestObject('A String', 1, []))
    }

    @Test
    void "reflective codec respects int range"() {
        assertOptional(CODEC.parse(ObjectOps.INSTANCE, [
                strValue: 'A string',
                intValue: 100,
                listValue: 'ABCDEFG'
        ]).error()).isPresent()
    }

    @POJO
    @ToString
    @TupleConstructor
    @EqualsAndHashCode
    @ExcludeFields('metaClass')
    static class TestObject {
        final @CodecSerialize(serializedName = 'strValue') String stringValue
        final @Range(intMax = 20) int intValue
        final @SingleOrList @OrEmpty List<String> listValue
    }
}
