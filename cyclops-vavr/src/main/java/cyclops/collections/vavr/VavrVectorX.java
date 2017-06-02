package cyclops.collections.vavr;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyPVectorX;
import com.aol.cyclops2.types.Unwrapable;
import cyclops.collections.immutable.VectorX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.PVector;


import io.vavr.collection.Vector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VavrVectorX<T> extends AbstractList<T> implements PVector<T>, Unwrapable {

    @Override
    public <R> R unwrap() {
        return (R)vector;
    }

    public static <T> VectorX<T> copyFromCollection(CollectionX<T> vec) {

        return VavrVectorX.<T>empty()
                .plusAll(vec);

    }
    /**
     * Create a LazyPVectorX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPVectorX
     */
    public static <T> LazyPVectorX<T> fromStream(Stream<T> stream) {
        return new LazyPVectorX<T>(null, ReactiveSeq.fromStream(stream),toPVector());
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
        return Reducer.<PVector<T>> of(VavrVectorX.emptyPVector(), (final PVector<T> a) -> b -> a.plusAll(b), (final T x) -> VavrVectorX.singleton(x));
    }
    public static <T> VavrVectorX<T> emptyPVector(){
        return new VavrVectorX<>(Vector.empty());
    }
    public static <T> LazyPVectorX<T> empty(){
        return fromPVector(new VavrVectorX<>(Vector.empty()), toPVector());
    }
    private static <T> LazyPVectorX<T> fromPVector(PVector<T> vec, Reducer<PVector<T>> pVectorReducer) {
        return new LazyPVectorX<T>(vec,null, pVectorReducer);
    }
    public static <T> LazyPVectorX<T> singleton(T t){
        return fromPVector(new VavrVectorX<>(Vector.of(t)), toPVector());
    }
    public static <T> LazyPVectorX<T> of(T... t){
        return fromPVector(new VavrVectorX<>(Vector.of(t)), toPVector());
    }
    public static <T> LazyPVectorX<T> ofAll(Vector<T> t){
        return fromPVector(new VavrVectorX<>(t), toPVector());
    }
    public static <T> LazyPVectorX<T> PVector(Vector<T> q) {
        return fromPVector(new VavrVectorX<T>(q), toPVector());
    }
    @SafeVarargs
    public static <T> LazyPVectorX<T> PVector(T... elements){
        return fromPVector(of(elements),toPVector());
    }
    @Wither
    private final Vector<T> vector;

    @Override
    public PVector<T> plus(T e) {
        return withVector(vector.append(e));
    }

    @Override
    public PVector<T> plusAll(Collection<? extends T> list) {
        return withVector(vector.appendAll(list));
    }

    @Override
    public PVector<T> with(int i, T e) {
        return withVector(vector.insert(i,e));
    }

    @Override
    public PVector<T> plus(int i, T e) {
        return withVector(vector.insert(i,e));
    }

    @Override
    public PVector<T> plusAll(int i, Collection<? extends T> list) {
        return withVector(vector.insertAll(i,list));
    }

    @Override
    public PVector<T> minus(Object e) {
        return withVector(vector.remove((T)e));
    }

    @Override
    public PVector<T> minusAll(Collection<?> list) {
        return withVector(vector.removeAll((Collection)list));
    }

    @Override
    public PVector<T> minus(int i) {
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