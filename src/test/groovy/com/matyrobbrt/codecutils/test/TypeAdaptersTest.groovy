package com.matyrobbrt.codecutils.test

import com.google.gson.reflect.TypeToken
import com.matyrobbrt.codecutils.api.CodecCreator
import com.matyrobbrt.codecutils.api.CodecTypeAdapter
import com.matyrobbrt.codecutils.api.annotation.CodecSerialize
import com.matyrobbrt.codecutils.api.annotation.UseAsAdapter
import com.matyrobbrt.codecutils.impl.types.FutureTypeAdapter
import com.matyrobbrt.codecutils.ops.ObjectOps
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

@POJO
@CompileStatic
class TypeAdaptersTest {
    static final CodecCreator CREATOR = CodecCreator.create()

    @Test
    void "can create adapter for object with CODEC field"() {
        final CodecTypeAdapter<SomeThing> codec = ((FutureTypeAdapter<SomeThing>)CREATOR.getAdapter(SomeThing)).adapter().get()
        assertThat(codec).isInstanceOf(CodecTypeAdapter.WrappingCodec)
        assertThat(codec.asCodec()).isEqualTo(SomeThing.CODEC)
    }

    @Test
    void "can create adapter for object with @UseAsAdapter field"() {
        final CodecTypeAdapter<SomeThing2> codec = ((FutureTypeAdapter<SomeThing2>)CREATOR.getAdapter(SomeThing2)).adapter().get()
        assertThat(codec).isInstanceOf(CodecTypeAdapter.WrappingCodec)
        assertThat(codec.asCodec()).isEqualTo(SomeThing2.MY_CODEC)
    }

    @Test
    void "enum value can be serialized"() {
        assertThat(TestEnum.CODEC.parse(ObjectOps.INSTANCE, 'AValue').result())
            .hasValue(TestEnum.AValue)
    }

    @Test
    void "enum value with custom name can be serialized"() {
        assertThat(TestEnum.CODEC.parse(ObjectOps.INSTANCE, 'AnotherValue').result())
            .hasValue(TestEnum.OtherValue)
    }

    private static final Codec<Either<Integer, String>> EITHER_CODEC = CREATOR.getAdapter(new TypeToken<Either<Integer, String>>() {}).asCodec()

    @Test
    void "either codec can serialize"() {
        assertThat(EITHER_CODEC.encodeStart(ObjectOps.INSTANCE, Either.left(1555)).result())
                .hasValue(1555)
        assertThat(EITHER_CODEC.encodeStart(ObjectOps.INSTANCE, Either.right('some str')).result())
                .hasValue('some str')
    }

    @Test
    void "either codec can deserialize"() {
        assertThat(EITHER_CODEC.parse(ObjectOps.INSTANCE, 12).result())
            .hasValue(Either.left(12))
        assertThat(EITHER_CODEC.parse(ObjectOps.INSTANCE, 'some string').result())
            .hasValue(Either.right('some string'))
    }

    static record SomeThing() {
        static final Codec<SomeThing> CODEC = Codec.unit(new SomeThing())
    }
    static record SomeThing2() {
        @UseAsAdapter
        static final Codec<SomeThing2> MY_CODEC = Codec.unit(new SomeThing2())
    }

    static enum TestEnum {
        AValue,
        @CodecSerialize(serializedName = 'AnotherValue') OtherValue

        static final Codec<TestEnum> CODEC = CREATOR.getCodec(TestEnum)
    }
}
