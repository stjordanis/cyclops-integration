package com.aol.cyclops.scala.collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.PVector;

import com.aol.cyclops.Reducer;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.reactor.collections.extensions.persistent.LazyPVectorX;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.Wither;
import reactor.core.publisher.Flux;
import scala.collection.GenTraversableOnce;
import scala.collection.generic.CanBuildFrom;
import scala.collection.immutable.Vector;
import scala.collection.immutable.Vector$;
import scala.collection.immutable.VectorBuilder;
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalaPVector<T> extends AbstractList<T> implements PVector<T> {
    
    /**
     * Create a LazyPVectorX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPVectorX
     */
    public static <T> LazyPVectorX<T> fromStream(Stream<T> stream) {
        return new LazyPVectorX<T>(
                                   Flux.from(ReactiveSeq.fromStream(stream)),toPVector());
    }

    /**
     * Create a LazyPVectorX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPVectorX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyPVectorX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyPVectorX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyPVectorX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyPVectorX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPVectorX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyPVectorX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                      .limit(limit));
    }

    /**
     * Create a LazyPVectorX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyPVectorX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                      .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * PVector<Integer> q = JSPVector.<Integer>toPVector()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for PVector
     */
    public static <T> Reducer<PVector<T>> toPVector() {
        return Reducer.<PVector<T>> of(ScalaPVector.empty(), (final PVector<T> a) -> b -> a.plusAll(b), (final T x) -> ScalaPVector.singleton(x));
    }
    
    public static <T> PVector<T> empty(){
        return LazyPVectorX.fromPVector(new ScalaPVector<>(Vector$.MODULE$.empty()), toPVector());
    }
    public static <T> PVector<T> singleton(T t){
        Vector<T> result = Vector$.MODULE$.empty();
        return LazyPVectorX.fromPVector(new ScalaPVector<>(result.appendFront(t)), toPVector());
    }
    public static <T> LazyPVectorX<T> of(T... t){
        VectorBuilder<T> vb = new VectorBuilder<T>();
        for(T next : t)
            vb.$plus$eq(next);
        Vector<T> vec = vb.result();
        return LazyPVectorX.fromPVector(new ScalaPVector<>(vec), toPVector());
    }
    public static <T> LazyPVectorX<T> PVector(Vector<T> q) {
        return LazyPVectorX.fromPVector(new ScalaPVector<T>(q), toPVector());
    }
    @SafeVarargs
    public static <T> LazyPVectorX<T> PVector(T... elements){
        return LazyPVectorX.fromPVector(of(elements),toPVector());
    }
    @Wither
    private final Vector<T> vector;

    @Override
    public PVector<T> plus(T e) {
        return withVector(vector.appendBack(e));
    }

    @Override
    public PVector<T> plusAll(Collection<? extends T> list) {
        Vector<T> vec = vector;
        for(T next :  list){
            vec = vec.appendBack(next);
        }
        
        
        return withVector(vec);
     }
 
    private PVector<T> plusAllVec(Vector<? extends T> list) {
        Vector<T> vec = vector;
        for(T next :  list){
            vec = vec.append(next);
        }
        return withVector(vec);
     }

    @Override
    public PVector<T> with(int i, T e) {
  
        return withVector(vector.updateAt(i,e));
    }

    @Override
    public PVector<T> plus(int i, T e) {
        return LazyPVectorX.fromPVector(this,toPVector()).insertAt(  ).
       
    }

    @Override
    public PVector<T> plusAll(int i, Collection<? extends T> list) {
        return withVector(vector.take(i)).plusAll(list).plusAllVec(vector.drop(i));
    }

    @Override
    public PVector<T> minus(Object e) {
        return LazyPVectorX.fromPVector(this,toPVector()).filter(i->Objects.equals(i,e));
    }

    @Override
    public PVector<T> minusAll(Collection<?> list) {
        return LazyPVectorX.fromPVector(this,toPVector()).removeAll((Iterable<T>)list);
    }

    @Override
    public PVector<T> minus(int i) {
        vector.
        return withVector(vector.removeAt(i));
    }

    @Override
    public PVector<T> subList(int start, int end) {
        return withVector(vector.subSequence(start, end));
    }

    @Override
    public T get(int index) {
        return vector.get(index);
    }

    @Override
    public int size() {
        return vector.size();
    }

   
}