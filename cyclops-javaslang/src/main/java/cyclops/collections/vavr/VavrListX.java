package cyclops.collections.vavr;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyLinkedListX;
import cyclops.collections.immutable.LinkedListX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.PStack;


import javaslang.collection.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VavrListX<T> extends AbstractList<T> implements PStack<T> {
    public static <T> LinkedListX<T> copyFromCollection(CollectionX<T> vec) {

        return VavrListX.<T>empty()
                .plusAll(vec);

    }
    /**
     * Create a LazyLinkedListX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyLinkedListX
     */
    public static <T> LazyLinkedListX<T> fromStream(Stream<T> stream) {
        Reducer<PStack<T>> p = toPStack();
        return new LazyLinkedListX<T>(null, ReactiveSeq.fromStream(stream),p);
    }

    /**
     * Create a LazyLinkedListX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyLinkedListX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyLinkedListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyLinkedListX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyLinkedListX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyLinkedListX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                      .limit(limit));
    }

    /**
     * Create a LazyLinkedListX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyLinkedListX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                      .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * PVector<Integer> q = JSPStack.<Integer>toPStack()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for PVector
     */
    public static <T> Reducer<PStack<T>> toPStack() {
        return Reducer.<PStack<T>> of(VavrListX.emptyPStack(), (final PStack<T> a) -> b -> a.plusAll(b), (final T x) -> VavrListX.singleton(x));
    }
    
    public static <T> VavrListX<T> emptyPStack(){
        return new VavrListX<T>(List.empty());
    }
    public static <T> LazyLinkedListX<T> empty(){
        return fromPStack(new VavrListX<T>(List.empty()), toPStack());
    }
    private static <T> LazyLinkedListX<T> fromPStack(PStack<T> s, Reducer<PStack<T>> pStackReducer) {
        return new LazyLinkedListX<T>(s,null, pStackReducer);
    }
    public static <T> LazyLinkedListX<T> singleton(T t){
        return fromPStack(new VavrListX<T>(List.of(t)), toPStack());
    }
    public static <T> LazyLinkedListX<T> of(T... t){
        return fromPStack(new VavrListX<T>(List.of(t)), toPStack());
    }
    public static <T> LazyLinkedListX<T> ofAll(List<T> q){
        return fromPStack(new VavrListX<T>(q), toPStack());
    }
    public static <T> LazyLinkedListX<T> PStack(List<T> q) {
        return fromPStack(new VavrListX<>(q), toPStack());
    }
    @SafeVarargs
    public static <T> LazyLinkedListX<T> PStack(T... elements){
        return fromPStack(of(elements),toPStack());
    }
    @Wither
    private final List<T> list;

    @Override
    public PStack<T> plus(T e) {
        return withList(list.prepend(e));
    }

    @Override
    public PStack<T> plusAll(Collection<? extends T> l) {
        List<T> use = list;
        for(T next :  l)
            use = use.prepend(next);
        return withList(use);
    }

    @Override
    public PStack<T> with(int i, T e) {
        List<T> front = list.take(i);
        List<T> back = list.drop(i);
        
        return withList(back.prepend(e).prependAll(front));
    }

    @Override
    public PStack<T> plus(int i, T e) {
        return withList(list.insert(i,e));
    }

    @Override
    public PStack<T> plusAll(int i, Collection<? extends T> l) {
       //use same behaviour as pCollections
        List<T> use = list;
        for(T next :  l)
            use = use.insert(i,next);
        return withList(use);
    }

    @Override
    public PStack<T> minus(Object e) {
        return withList(list.remove((T)e));
    }

    @Override
    public PStack<T> minusAll(Collection<?> l) {
        return withList(list.removeAll((Collection)l));
    }

    @Override
    public PStack<T> minus(int i) {
        return withList(list.removeAt(i));
    }

    @Override
    public PStack<T> subList(int start, int end) {
        return withList(list.subSequence(start, end));
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public org.pcollections.PStack<T> subList(int start) {
       return withList(list.subSequence(start));
    }

   
}
