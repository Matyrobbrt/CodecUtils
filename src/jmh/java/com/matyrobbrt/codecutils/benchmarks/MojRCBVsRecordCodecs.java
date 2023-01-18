package com.matyrobbrt.codecutils.benchmarks;

import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.ops.ObjectOps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.All)
public class MojRCBVsRecordCodecs {

    private static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 2;
    private static final int ITERATION_COUNT = 5;
    private static final int THREAD_COUNT = 2;

    private Map<Object, Object> structure;
    private Codec<RecordObject> mojRCB;
    private Codec<RecordObject> recordCodec;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        this.mojRCB = RecordCodecBuilder.create(in -> in.group(
                Codec.STRING.fieldOf("stringValue").forGetter(RecordObject::stringValue)
        ).apply(in, RecordObject::new));
        this.recordCodec = CodecCreator.create().getCodec(RecordObject.class);

        this.structure = Map.of(
                "stringValue", "Some String"
        );
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public RecordObject testMojRCB() {
        return mojRCB.parse(ObjectOps.INSTANCE, structure).get().orThrow();
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public RecordObject testRecordCodec() {
        return recordCodec.parse(ObjectOps.INSTANCE, structure).get().orThrow();
    }

    public record RecordObject(String stringValue) {

    }
}
