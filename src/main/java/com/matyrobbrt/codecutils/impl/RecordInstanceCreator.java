package com.matyrobbrt.codecutils.impl;

import com.matyrobbrt.codecutils.InstanceCreator;
import com.matyrobbrt.codecutils.codecs.FieldsCodec;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;
import com.matyrobbrt.codecutils.invoke.internal.ObjectCreatorMetafactory;
import com.matyrobbrt.codecutils.invoke.Reflection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.lang.invoke.CallSite;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Optional;

public final class RecordInstanceCreator<R> implements InstanceCreator<R> {
    private final Class<R> recordClass;
    private final Instantiator.Factory instantiatorFactor;

    public RecordInstanceCreator(Class<R> recordClass, Instantiator.Factory instantiatorFactor) {
        this.recordClass = recordClass;
        this.instantiatorFactor = instantiatorFactor;
    }

    private RecordData<R> data;
    public void buildData() throws Throwable {
        final Object2IntMap<String> indices = new Object2IntOpenHashMap<>();
        final RecordComponent[] components = recordClass.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            indices.put(components[i].getName(), i);
        }
        data = new RecordData<>(indices, instantiatorFactor.create(recordClass));
    }

    @Override
    public Acceptor<R> create() throws Throwable {
        if (data == null) buildData();
        final Object[] args = new Object[data.indices.size()];
        return new Acceptor<>() {
            @Override
            public <T> void accept(FieldsCodec.BoundField<R, T> field, T value) throws Throwable {
                if (field instanceof FieldsCodec.BoundField.ForRecordComponent<R,T> recordComponent) {
                    args[data.indices.getInt(recordComponent.getFieldName())] = (recordComponent.isOptionalType() ? Optional.ofNullable(value) : value);
                } else {
                    throw new IllegalArgumentException("Allocating instance creators require record component based bounds!");
                }
            }

            @Override
            public <T> void acceptPartial(FieldsCodec.BoundField<R, T> field, T value) throws Throwable {
                this.accept(field, value);
            }

            @Override
            public R finish() throws Throwable {
                return data.instantiator.make(args);
            }

            @Override
            public R finishNow() throws Throwable { // We can't have partials with records
                return null;
            }
        };
    }

    public record RecordData<T>(Object2IntMap<String> indices, Instantiator<T> instantiator) {

    }

    public interface Instantiator<T> {
        T make(Object[] args) throws Throwable;

        static Factory ctor() {
            return new Factory() {
                @SuppressWarnings("unchecked")
                @Override
                public <T> Instantiator<T> create(Class<T> clazz) throws Throwable {
                    final CallSite cs = new ObjectCreatorMetafactory(
                            Reflection.getLookup(clazz),
                            getFullCtor(clazz)
                    ).buildCallSite();
                    final ObjectCreator<T> invoker = (ObjectCreator<T>) cs.getTarget().invokeExact();
                    return invoker::invoke;
                }
            };
        }

        interface Factory {
            <T> Instantiator<T> create(Class<T> clazz) throws Throwable;
        }
    }

    public static <T> Constructor<T> getFullCtor(Class<T> clazz) throws NoSuchMethodException {
        final RecordComponent[] comp = clazz.getRecordComponents();
        final Class<?>[] params = new Class<?>[comp.length];
        for (int i = 0; i < params.length; i++) params[i] = comp[i].getType();
        return clazz.getDeclaredConstructor(params);
    }
}
