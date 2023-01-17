package com.matyrobbrt.codecutils.impl;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.CodecCreator;
import com.matyrobbrt.codecutils.annotation.CodecSerialize;
import com.matyrobbrt.codecutils.annotation.ExcludeFields;
import com.matyrobbrt.codecutils.codecs.FieldsCodec;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class CodecGenerator {
    public static final RecordInstanceCreator.Instantiator.Factory FACTORY = RecordInstanceCreator.Instantiator.ctor();

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> generateRecord(CodecCreator creator, TypeToken<T> recordType) throws Throwable {
        final List<FieldsCodec.BoundField<T, ?>> fields = new ArrayList<>();
        for (final RecordComponent comp : recordType.getRawType().getRecordComponents()) {
            final FieldDataResolvers.FieldData<?> data = creator.getFieldDataResolvers().resolve(comp, recordType);
            fields.add(bind(comp, data));
        }
        return new FieldsCodec<>(fields, new RecordInstanceCreator(recordType.getRawType(), FACTORY)).codec();
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> generateClass(CodecCreator creator, TypeToken<T> recordType) throws Throwable {
        final List<FieldsCodec.BoundField<T, ?>> fields = new ArrayList<>();
        final Set<String> toExclude = Arrays.stream(Optional.ofNullable(recordType.getRawType().getAnnotation(ExcludeFields.class))
                .map(ExcludeFields::value).orElse(new String[0])).collect(Collectors.toSet());
        for (final Field field : recordType.getRawType().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || toExclude.contains(field.getName())) continue;
            if (Optional.ofNullable(field.getAnnotation(CodecSerialize.class)).map(CodecSerialize::exclude).orElse(false)) continue;

            final FieldDataResolvers.FieldData<?> data = creator.getFieldDataResolvers().resolve(field, recordType);
            fields.add(bind(field, data));
        }
        return new FieldsCodec<>(fields, new AllocatingInstanceCreator(creator.getDefaultCreators()::createNoArgs, recordType.getRawType())).codec();
    }

    @SuppressWarnings("unchecked")
    private static <T, Z> FieldsCodec.BoundField<T, Z> bind(RecordComponent comp, FieldDataResolvers.FieldData<Z> data) throws Throwable {
        final Codec<Z> codec = (Codec<Z>) data.typeAdapter().asCodec();
        return new FieldsCodec.BoundField.ForRecordComponent<>(
                data.name(), comp.getName(),
                !data.optional(), comp.getType() == Optional.class, data.defaultValue(),
                codec, codec,
                Reflection.reader(comp)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T, Z> FieldsCodec.BoundField<T, Z> bind(Field field, FieldDataResolvers.FieldData<Z> data) throws Throwable {
        final Codec<Z> codec = (Codec<Z>) data.typeAdapter().asCodec();
        return new FieldsCodec.BoundField.ForField<>(
                data.name(), !data.optional(), field.getType() == Optional.class,
                data.defaultValue(), codec, codec,
                Reflection.findBestReadStrategy(field), Reflection.findBestWriteStrategy(field)
        );
    }
}
