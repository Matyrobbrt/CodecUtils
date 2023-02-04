package com.matyrobbrt.codecutils.api.ops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.api.exception.CannotCreateAdapter;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;

import java.io.EOFException;
import java.io.IOException;

public class CodecBasedGsonTypeAdapter<T> extends TypeAdapter<T> {
    private final CodecTypeAdapter<T> adapter;

    public CodecBasedGsonTypeAdapter(CodecTypeAdapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        final JsonElement encoded = adapter.encode(value, JsonOps.INSTANCE, JsonOps.INSTANCE.empty())
                .getOrThrow(false, e -> {
                    throw new EncodingException(e);
                });
        TypeAdapters.JSON_ELEMENT.write(out, encoded);
    }

    @Override
    public T read(JsonReader in) {
        final JsonElement elem = parse(in);
        if (elem.isJsonNull()) return null;
        return adapter.decode(JsonOps.INSTANCE, elem)
                .map(Pair::getFirst).getOrThrow(false, e -> {
                    throw new DecodingException(e);
                });
    }

    public static JsonElement parse(JsonReader reader) throws JsonParseException {
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return TypeAdapters.JSON_ELEMENT.read(reader);
        } catch (EOFException e) {
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            throw new JsonSyntaxException(e);
        } catch (MalformedJsonException | NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public static TypeAdapterFactory codecCreatorFactory(CodecCreator creator) {
        return new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                try {
                    final CodecTypeAdapter<T> adapter = creator.getAdapter(type);
                    return new CodecBasedGsonTypeAdapter<>(adapter);
                } catch (CannotCreateAdapter ex) {
                    return null;
                }
            }
        };
    }

    public static final class DecodingException extends RuntimeException {
        public DecodingException(String m) {
            super(m);
        }
    }
    public static final class EncodingException extends RuntimeException {
        public EncodingException(String m) {
            super(m);
        }
    }
}
