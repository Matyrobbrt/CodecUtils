package com.matyrobbrt.codecutils.codecs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Codecs {
    @SuppressWarnings("rawtypes")
    public static final Function SUCCESS = success();
    public static <T> Function<T, DataResult<T>> success() {
        return DataResult::success;
    }

    public static <T> Function<T, DataResult<T>> verifier(Predicate<T> verifier, Function<T, String> message) {
        return t -> verifier.test(t) ? DataResult.success(t) : DataResult.error(message.apply(t));
    }

    public static <T> Function<T, DataResult<T>> checkRangeInclusive(Comparator<T> comparator, T minInclusive, T maxInclusive) {
        return verifier(t -> comparator.compare(t, minInclusive) >= 0 && comparator.compare(t, maxInclusive) <= 0, obj -> "Value " + obj + " outside of range [" + minInclusive + ", " + maxInclusive + "]");
    }

    public static <T> Codec<T> verify(Codec<T> codec, Predicate<T> verifier, Function<T, String> message) {
        final Function<T, DataResult<T>> res = verifier(verifier, message);
        return codec.flatXmap(res, res);
    }

    public static <T> Codec<T> inRange(Codec<T> codec, Comparator<T> comparator, T minInclusive, T maxInclusive) {
        final Function<T, DataResult<T>> ver = checkRangeInclusive(comparator, minInclusive, maxInclusive);
        return codec.flatXmap(ver, ver);
    }

    public static <T> Codec<List<T>> singleOrList(Codec<T> codec) {
        return Codec.either(codec, codec.listOf()).xmap(
                it -> it.map(List::of, Function.identity()),
                it -> it.size() == 1 ? Either.left(it.get(0)) : Either.right(it)
        );
    }
}
