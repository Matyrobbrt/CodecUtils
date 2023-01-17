package com.matyrobbrt.codecutils.codecs;

import com.matyrobbrt.codecutils.invoke.FieldReader;
import com.matyrobbrt.codecutils.invoke.FieldWriter;
import com.matyrobbrt.codecutils.InstanceCreator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FieldsCodec<Z> extends MapCodec<Z> {
    private final List<BoundField<Z, ?>> fields;
    private final InstanceCreator<Z> instanceCreator;

    public FieldsCodec(List<BoundField<Z, ?>> fields, InstanceCreator<Z> instanceCreator) {
        this.fields = fields;
        this.instanceCreator = instanceCreator;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return fields.stream()
                .map(f -> ops.createString(f.getName()));
    }

    @Override
    public <T> DataResult<Z> decode(DynamicOps<T> ops, MapLike<T> input) {
        final List<String> messages = new ArrayList<>();
        try {
            final InstanceCreator.Acceptor<Z> acceptor = instanceCreator.create();
            for (final BoundField<Z, ?> field : fields) {
                try {
                    final String message = decodeField(field, acceptor, ops, input);
                    if (message != null) {
                        messages.add(message);
                    }
                } catch (Throwable e) {
                    messages.add(e.getMessage());
                }
            }

            if (messages.isEmpty()) {
                return DataResult.success(acceptor.finish());
            }

            final Z partial = acceptor.finishNow();
            final String msg = String.join("; ", messages);
            if (partial == null) {
                return DataResult.error(msg);
            }
            return DataResult.error(msg, partial);
        } catch (Throwable e) {
            return DataResult.error(e.getMessage());
        }
    }

    private <T, O> @Nullable String decodeField(BoundField<Z, T> field, InstanceCreator.Acceptor<Z> acceptor, DynamicOps<O> ops, MapLike<O> input) throws Throwable {
        final DataResult<T> result = field.decode(ops, input);
        if (result == null) return null;

        if (result.error().isPresent()) {
            final DataResult.PartialResult<T> partialResult = result.error().get();
            final T partial = result.resultOrPartial(e -> {}).orElse(null);
            if (partial != null) acceptor.acceptPartial(field, partial);
            return partialResult.message();
        }
        acceptor.accept(field, result.get().orThrow());
        return null;
    }

    @Override
    public <T> RecordBuilder<T> encode(Z input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        for (final BoundField<Z, ?> field : fields) {
            field.encode(input, ops, prefix);
        }
        return prefix;
    }

    public interface BoundField<I, T> {
        String getName();
        <Z> void encode(I input, DynamicOps<Z> ops, RecordBuilder<Z> prefix);
        @Nullable
        <Z> DataResult<T> decode(DynamicOps<Z> ops, MapLike<Z> map);

        class ForField<I, T> implements BoundField<I, T> {
            private final String name;
            private final boolean required;
            private final boolean isOptionalType;
            @Nullable
            private final Supplier<T> defaultValue;
            private final @Nullable Encoder<T> encoder;
            private final @Nullable Decoder<T> decoder;
            private final FieldReader<I, T> reader;
            private final FieldWriter<I, T> writer;

            public ForField(String name, boolean required, boolean isOptionalType, @Nullable Supplier<T> defaultValue, Encoder<T> encoder, Decoder<T> decoder, FieldReader<I, T> reader, FieldWriter<I, T> writer) {
                this.name = name;
                this.required = required;
                this.isOptionalType = isOptionalType;
                this.defaultValue = defaultValue;
                this.encoder = encoder;
                this.decoder = decoder;
                this.reader = isOptionalType ? FieldReader.optionalUnwrap(reader) : reader;
                this.writer = isOptionalType ? FieldWriter.optionalWrap(writer) : writer;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public <Z> void encode(I input, DynamicOps<Z> ops, RecordBuilder<Z> prefix) {
                if (encoder == null) return;

                try {
                    final T value = reader.read(input);
                    if (value == null) return; // Codecs don't do nulls
                    prefix.add(name, value, encoder);
                } catch (Throwable e) {
                    prefix.withErrorsFrom(DataResult.error(e.getMessage()));
                }
            }

            @Override
            public <Z> DataResult<T> decode(DynamicOps<Z> ops, MapLike<Z> map) {
                if (decoder == null) return null;

                final Z mapVal = map.get(name);
                if (mapVal == null) {
                    if (required) {
                        return DataResult.error("Missing required key: " + name);
                    }
                    return DataResult.success(defaultValue == null ? null : defaultValue.get());
                }
                return decoder.decode(ops, mapVal).map(Pair::getFirst);
            }

            public FieldWriter<I, T> getWriter() {
                return writer;
            }

            public boolean isOptionalType() {
                return isOptionalType;
            }
        }

        class ForRecordComponent<I, T> extends ForField<I, T> {
            private final String fieldName;
            @SuppressWarnings("unchecked")
            public ForRecordComponent(String name, String fieldName, boolean required, boolean isOptionalType, @Nullable Supplier<T> defaultValue, Encoder<T> encoder, Decoder<T> decoder, FieldReader<I, T> reader) {
                super(name, required, isOptionalType, defaultValue, encoder, decoder, reader, FieldWriter.DUMMY);
                this.fieldName = fieldName;
            }

            public String getFieldName() {
                return fieldName;
            }
        }

    }
}
