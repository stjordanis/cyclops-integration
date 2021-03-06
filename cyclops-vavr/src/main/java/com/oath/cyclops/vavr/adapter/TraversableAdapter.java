package com.oath.cyclops.vavr.adapter;

import com.oath.cyclops.anym.extensability.AbstractMonadAdapter;
import cyclops.monads.VavrWitness;

import cyclops.monads.AnyM;
import cyclops.reactive.ReactiveSeq;
import io.vavr.collection.Traversable;
import lombok.AllArgsConstructor;

import java.util.function.Function;
import java.util.function.Predicate;


@AllArgsConstructor
public abstract class TraversableAdapter<W extends VavrWitness.TraversableWitness<W>> extends AbstractMonadAdapter<W> {


    W instance;
    @Override
    public <T> Iterable<T> toIterable(AnyM<W, T> t) {
        return ()->stream(t).iterator();
    }

    @Override
    public <T, R> AnyM<W, R> ap(AnyM<W,? extends Function<? super T,? extends R>> fn, AnyM<W, T> apply) {
        Traversable<T> f = stream(apply);
        Traversable<? extends Function<? super T, ? extends R>> fnF = stream(fn);
        return unitIterable(ReactiveSeq.fromIterable(fnF).zip(f, (a, b) -> a.apply(b)));

    }

    @Override
    public <T> AnyM<W, T> filter(AnyM<W, T> t, Predicate<? super T> fn) {
        return anyM(stream(t).filter(fn));
    }

    <T> Traversable<T> stream(AnyM<W,T> anyM){
        return anyM.unwrap();
    }
    public abstract <T> Traversable<T> emptyTraversable();
    public abstract <T> Traversable<T> singletonTraversable(T value);
    public abstract <T> Traversable<T> traversableFromIterable(Iterable<T> value);
    @Override
    public <T> AnyM<W, T> empty() {
        return anyM(emptyTraversable());
    }

    private <T> AnyM<W,T> anyM(Traversable<T> t){
        return AnyM.ofSeq(t,instance);
    }



    @Override
    public <T, R> AnyM<W, R> flatMap(AnyM<W, T> t,
                                     Function<? super T, ? extends AnyM<W, ? extends R>> fn) {
        return anyM(stream(t).flatMap(a->stream(fn.apply(a))));

    }

    @Override
    public <T> AnyM<W, T> unitIterable(Iterable<T> it)  {
        return anyM(traversableFromIterable(it));
    }

    @Override
    public <T> AnyM<W, T> unit(T o) {
        return anyM(singletonTraversable(o));
    }

    @Override
    public <T, R> AnyM<W, R> map(AnyM<W, T> t, Function<? super T, ? extends R> fn) {
        return anyM(stream(t).map(fn));
    }
}
