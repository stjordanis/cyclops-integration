package cyclops.companion.vavr;

import cyclops.control.*;
import cyclops.monads.VavrWitness.queue;
import io.vavr.Lazy;
import io.vavr.collection.*;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.*;
import com.aol.cyclops.vavr.hkt.*;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Optionals;
import cyclops.monads.*;
import cyclops.monads.VavrWitness.*;
import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.types.anyM.AnyMSeq;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;
import cyclops.monads.Witness.*;
import cyclops.reactive.ReactiveSeq;
import cyclops.typeclasses.*;
import com.aol.cyclops.vavr.hkt.HashSetKind;

import cyclops.control.Maybe;
import cyclops.control.Either;
import cyclops.monads.AnyM;
import cyclops.monads.VavrWitness;
import cyclops.monads.VavrWitness.hashSet;
import cyclops.monads.WitnessType;

import cyclops.monads.XorM;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import cyclops.data.tuple.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import static com.aol.cyclops.vavr.hkt.HashSetKind.narrowK;
import static com.aol.cyclops.vavr.hkt.HashSetKind.widen;


public class HashSets {

    public static  <W1,T> Coproduct<W1,hashSet,T> coproduct(HashSet<T> type, InstanceDefinitions<W1> def1){
        return Coproduct.of(Either.right(widen(type)),def1, Instances.definitions());
    }
    public static  <W1,T> Coproduct<W1,hashSet,T> coproduct(InstanceDefinitions<W1> def1,T... values){
        return Coproduct.of(Either.right(HashSetKind.just(values)),def1, Instances.definitions());
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,hashSet,T> xorM(HashSet<T> type){
        return XorM.right(anyM(type));
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,hashSet,T> xorM(T... values){
        return xorM(HashSet.of(values));
    }
    public static <T> AnyMSeq<hashSet,T> anyM(HashSet<T> option) {
        return AnyM.ofSeq(option, hashSet.INSTANCE);
    }

    public static  <T,R> HashSet<R> tailRec(T initial, Function<? super T, ? extends HashSet<? extends Either<T, R>>> fn) {
        HashSet<Either<T, R>> next = HashSet.of(Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.flatMap(e -> e.fold(s -> {
                        newValue[0]=true;
                        return fn.apply(s); },
                    p -> {
                        newValue[0]=false;
                        return HashSet.of(e);
                    }));
            if(!newValue[0])
                break;

        }

        return next.filter(Either::isRight).map(Either::get);
    }
    public static  <T,R> HashSet<R> tailRecEither(T initial, Function<? super T, ? extends HashSet<? extends Either<T, R>>> fn) {
        HashSet<Either<T, R>> next = HashSet.of(Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.flatMap(e -> e.visit(s -> {
                        newValue[0]=true;
                        return fn.apply(s); },
                    p -> {
                        newValue[0]=false;
                        return HashSet.of(e);
                    }));
            if(!newValue[0])
                break;

        }

        return next.filter(Either::isRight).map(Either::get);
    }


    /**
     * Perform a For Comprehension over a Set, accepting 3 generating functions.
     * This results in a four level nested internal iteration over the provided Publishers.
     *
     *  <pre>
     * {@code
     *
     *   import static cyclops.HashSets.forEach4;
     *
    forEach4(IntSet.range(1,10).boxed(),
    a-> Set.iterate(a,i->i+1).limit(10),
    (a,b) -> Set.<Integer>of(a+b),
    (a,b,c) -> Set.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Set
     * @param value2 Nested Set
     * @param value3 Nested Set
     * @param value4 Nested Set
     * @param yieldingFunction  Generates a result per combination
     * @return Set with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Set<R> forEach4(Set<? extends T1> value1,
                                                               Function<? super T1, ? extends Set<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends Set<R2>> value3,
                                                               Function3<? super T1, ? super R1, ? super R2, ? extends Set<R3>> value4,
                                                               Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Set<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Set<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Set, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     *  import static com.oath.cyclops.reactor.Setes.forEach4;
     *
     *  forEach4(IntSet.range(1,10).boxed(),
    a-> Set.iterate(a,i->i+1).limit(10),
    (a,b) -> Set.<Integer>just(a+b),
    (a,b,c) -> Set.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Set
     * @param value2 Nested Set
     * @param value3 Nested Set
     * @param value4 Nested Set
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Set with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Set<R> forEach4(Set<? extends T1> value1,
                                                                 Function<? super T1, ? extends Set<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Set<R2>> value3,
                                                                 Function3<? super T1, ? super R1, ? super R2, ? extends Set<R3>> value4,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Set<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Set<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }

    /**
     * Perform a For Comprehension over a Set, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     * import static HashSets.forEach3;
     *
     * forEach(IntSet.range(1,10).boxed(),
    a-> Set.iterate(a,i->i+1).limit(10),
    (a,b) -> Set.<Integer>of(a+b),
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     *
     * @param value1 top level Set
     * @param value2 Nested Set
     * @param value3 Nested Set
     * @param yieldingFunction Generates a result per combination
     * @return Set with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Set<R> forEach3(Set<? extends T1> value1,
                                                         Function<? super T1, ? extends Set<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Set<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Set<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });


    }

    /**
     * Perform a For Comprehension over a Set, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     * import static HashSets.forEach;
     *
     * forEach(IntSet.range(1,10).boxed(),
    a-> Set.iterate(a,i->i+1).limit(10),
    (a,b) -> Set.<Integer>of(a+b),
    (a,b,c) ->a+b+c<10,
    Tuple::tuple)
    .toSetX();
     * }
     * </pre>
     *
     * @param value1 top level Set
     * @param value2 Nested publisher
     * @param value3 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T1, T2, R1, R2, R> Set<R> forEach3(Set<? extends T1> value1,
                                                         Function<? super T1, ? extends Set<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Set<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Set<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });
    }

    /**
     * Perform a For Comprehension over a Set, accepting an additonal generating function.
     * This results in a two level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     *  import static HashSets.forEach2;
     *  forEach(IntSet.range(1, 10).boxed(),
     *          i -> Set.range(i, 10), Tuple::tuple)
    .forEach(System.out::println);

    //(1, 1)
    (1, 2)
    (1, 3)
    (1, 4)
    ...
     *
     * }</pre>
     *
     * @param value1 top level Set
     * @param value2 Nested publisher
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Set<R> forEach2(Set<? extends T> value1,
                                                Function<? super T, Set<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });

    }

    /**
     *
     * <pre>
     * {@code
     *
     *   import static HashSets.forEach2;
     *
     *   forEach(IntSet.range(1, 10).boxed(),
     *           i -> Set.range(i, 10),
     *           (a,b) -> a>2 && b<10,
     *           Tuple::tuple)
    .forEach(System.out::println);

    //(3, 3)
    (3, 4)
    (3, 5)
    (3, 6)
    (3, 7)
    (3, 8)
    (3, 9)
    ...

     *
     * }</pre>
     *
     *
     * @param value1 top level Set
     * @param value2 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Set<R> forEach2(Set<? extends T> value1,
                                                Function<? super T, ? extends Set<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Set<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });
    }

    public static <T> Active<hashSet,T> allTypeclasses(HashSet<T> array){
        return Active.of(widen(array), HashSets.Instances.definitions());
    }
    public static <T,W2,R> Nested<hashSet,W2,R> mapM(HashSet<T> array, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        HashSet<Higher<W2, R>> e = array.map(fn);
        HashSetKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, HashSets.Instances.definitions(), defs);
    }
    /**
     * Companion class for creating Type Class instances for working with HashSets
     *
     */
    @UtilityClass
    public static class Instances {

        public static InstanceDefinitions<hashSet> definitions() {
            return new InstanceDefinitions<hashSet>() {

                @Override
                public <T, R> Functor<hashSet> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<hashSet> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<hashSet> applicative() {
                    return Instances.zippingApplicative();
                }

                @Override
                public <T, R> Monad<hashSet> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<hashSet>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<hashSet>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> MonadRec<hashSet> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<hashSet>> monadPlus(Monoid<Higher<hashSet, T>> m) {
                    return Maybe.just(Instances.monadPlus(m));
                }

                @Override
                public <C2, T> Traverse<hashSet> traverse() {
                    return Instances.traverse();
                }

                @Override
                public <T> Foldable<hashSet> foldable() {
                    return Instances.foldable();
                }

                @Override
                public <T> Maybe<Comonad<hashSet>> comonad() {
                    return Maybe.nothing();
                }

                @Override
                public <T> Maybe<Unfoldable<hashSet>> unfoldable() {
                    return Maybe.just(Instances.unfoldable());
                }
            };
        }
        /**
         *
         * Transform a hashSet, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  HashSetKind<Integer> hashSet = HashSets.functor().map(i->i*2, HashSetKind.widen(Set.of(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with HashSets
         * <pre>
         * {@code
         *   HashSetKind<Integer> hashSet = HashSets.unit()
        .unit("hello")
        .then(h->HashSets.functor().map((String v) ->v.length(), h))
        .convert(HashSetKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for HashSets
         */
        public static <T,R>Functor<hashSet> functor(){
            BiFunction<HashSetKind<T>,Function<? super T, ? extends R>,HashSetKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * HashSetKind<String> hashSet = HashSets.unit()
        .unit("hello")
        .convert(HashSetKind::narrowK);

        //Set.of("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for HashSets
         */
        public static <T> Pure<hashSet> unit(){
            return General.<hashSet,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.HashSetKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         *
        HashSets.zippingApplicative()
        .ap(widen(Set.of(l1(this::multiplyByTwo))),widen(Set.of(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * HashSetKind<Function<Integer,Integer>> setFn =HashSets.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(HashSetKind::narrowK);

        HashSetKind<Integer> hashSet = HashSets.unit()
        .unit("hello")
        .then(h->HashSets.functor().map((String v) ->v.length(), h))
        .then(h->HashSets.zippingApplicative().ap(setFn, h))
        .convert(HashSetKind::narrowK);

        //Set.of("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for HashSets
         */
        public static <T,R> Applicative<hashSet> zippingApplicative(){
            BiFunction<HashSetKind< Function<T, R>>,HashSetKind<T>,HashSetKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.HashSetKind.widen;
         * HashSetKind<Integer> hashSet  = HashSets.monad()
        .flatMap(i->widen(SetX.range(0,i)), widen(Set.of(1,2,3)))
        .convert(HashSetKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    HashSetKind<Integer> hashSet = HashSets.unit()
        .unit("hello")
        .then(h->HashSets.monad().flatMap((String v) ->HashSets.unit().unit(v.length()), h))
        .convert(HashSetKind::narrowK);

        //Set.of("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for HashSets
         */
        public static <T,R> Monad<hashSet> monad(){

            BiFunction<Higher<hashSet,T>,Function<? super T, ? extends Higher<hashSet,R>>,Higher<hashSet,R>> flatMap = Instances::flatMap;
            return General.monad(zippingApplicative(), flatMap);
        }
        public static <T> MonadRec<hashSet> monadRec(){
            return new MonadRec<hashSet>(){

                @Override
                public <T, R> Higher<hashSet, R> tailRec(T initial, Function<? super T, ? extends Higher<hashSet, ? extends Either<T, R>>> fn) {
                    return widen(tailRecEither(initial,fn.andThen(HashSetKind::narrowK).andThen(hs->hs.narrow())));
                }
            };
        }
        /**
         *
         * <pre>
         * {@code
         *  HashSetKind<String> hashSet = HashSets.unit()
        .unit("hello")
        .then(h->HashSets.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(HashSetKind::narrowK);

        //Set.of("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<hashSet> monadZero(){
            BiFunction<Higher<hashSet,T>,Predicate<? super T>,Higher<hashSet,T>> filter = Instances::filter;
            Supplier<Higher<hashSet, T>> zero = ()-> widen(HashSet.empty());
            return General.<hashSet,T,R>monadZero(monad(), zero,filter);
        }
        /**
         * <pre>
         * {@code
         *  HashSetKind<Integer> hashSet = HashSets.<Integer>monadPlus()
        .plus(HashSetKind.widen(Set.of()), HashSetKind.widen(Set.of(10)))
        .convert(HashSetKind::narrowK);
        //Set.of(10))
         *
         * }
         * </pre>
         * @return Type class for combining HashSets by concatenation
         */
        public static <T> MonadPlus<hashSet> monadPlus(){
            Monoid<HashSetKind<T>> m = Monoid.of(widen(HashSet.<T>empty()), Instances::concat);
            Monoid<Higher<hashSet,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<HashSetKind<Integer>> m = Monoid.of(HashSetKind.widen(Set.of()), (a,b)->a.isEmpty() ? b : a);
        HashSetKind<Integer> hashSet = HashSets.<Integer>monadPlus(m)
        .plus(HashSetKind.widen(Set.of(5)), HashSetKind.widen(Set.of(10)))
        .convert(HashSetKind::narrowK);
        //Set.of(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining HashSets
         * @return Type class for combining HashSets
         */
        public static <T> MonadPlus<hashSet> monadPlus(Monoid<Higher<hashSet,T>> m){
            Monoid<Higher<hashSet,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        public static <T> MonadPlus<hashSet> monadPlusK(Monoid<HashSetKind<T>> m){
            Monoid<Higher<hashSet,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<hashSet> traverse(){
            BiFunction<Applicative<C2>,HashSetKind<Higher<C2, T>>,Higher<C2, HashSetKind<T>>> sequenceFn = (ap, set) -> {

                Higher<C2,HashSetKind<T>> identity = ap.unit(widen(HashSet.empty()));

                BiFunction<Higher<C2,HashSetKind<T>>,Higher<C2,T>,Higher<C2,HashSetKind<T>>> combineToSet =   (acc, next) -> ap.apBiFn(ap.unit((a, b) -> concat(a, HashSetKind.just(b))),acc,next);

                BinaryOperator<Higher<C2,HashSetKind<T>>> combineSets = (a, b)-> ap.apBiFn(ap.unit((l1, l2)-> { return concat(l1,l2);}),a,b); ;

                return ReactiveSeq.fromIterable(set).reduce(identity,
                        combineToSet,
                        combineSets);


            };
            BiFunction<Applicative<C2>,Higher<hashSet,Higher<C2, T>>,Higher<C2, Higher<hashSet,T>>> sequenceNarrow  =
                    (a,b) -> HashSetKind.widen2(sequenceFn.apply(a, HashSetKind.narrowK(b)));
            return General.traverse(zippingApplicative(), sequenceNarrow);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = HashSets.foldable()
        .foldLeft(0, (a,b)->a+b, HashSetKind.widen(Set.of(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<hashSet> foldable(){
            return new Foldable<hashSet>() {
                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<hashSet, T> ds) {
                    return narrowK(ds).foldRight(monoid.zero(),monoid);
                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<hashSet, T> ds) {
                    return narrowK(ds).foldLeft(monoid.zero(),monoid);
                }

                @Override
                public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<hashSet, T> nestedA) {
                    return narrowK(nestedA).foldRight(mb.zero(),(a,b)->mb.apply(fn.apply(a),b));
                }
            };
        }

        private static  <T> HashSetKind<T> concat(HashSetKind<T> l1, HashSetKind<T> l2){
            return widen(l1.addAll(l2));
        }
        private <T> HashSetKind<T> of(T value){
            return widen(HashSet.of(value));
        }
        private static <T,R> HashSetKind<R> ap(HashSetKind<Function< T, R>> lt, HashSetKind<T> set){
            return widen(lt.toReactiveSeq().zip(set,(a, b)->a.apply(b)));
        }
        private static <T,R> Higher<hashSet,R> flatMap(Higher<hashSet,T> lt, Function<? super T, ? extends  Higher<hashSet,R>> fn){
            return widen(HashSetKind.narrowK(lt).flatMap(fn.andThen(HashSetKind::narrowK)));
        }
        private static <T,R> HashSetKind<R> map(HashSetKind<T> lt, Function<? super T, ? extends R> fn){
            return widen(lt.map(fn));
        }
        private static <T> HashSetKind<T> filter(Higher<hashSet,T> lt, Predicate<? super T> fn){
            return widen(HashSetKind.narrow(lt).filter(fn));
        }
        public static Unfoldable<hashSet> unfoldable(){
            return new Unfoldable<hashSet>() {
                @Override
                public <R, T> Higher<hashSet, R> unfold(T b, Function<? super T, Option<Tuple2<R, T>>> fn) {
                    return widen(ReactiveSeq.unfold(b,fn).collect(HashSet.collector()));

                }
            };
        }
    }
    public static interface SetNested{


        public static <T> Nested<hashSet,lazy,T> lazy(HashSet<Lazy<T>> type){
            return Nested.of(widen(type.map(LazyKind::widen)),Instances.definitions(),Lazys.Instances.definitions());
        }
        public static <T> Nested<hashSet,VavrWitness.tryType,T> setTry(HashSet<Try<T>> type){
            return Nested.of(widen(type.map(TryKind::widen)),Instances.definitions(),Trys.Instances.definitions());
        }
        public static <T> Nested<hashSet,VavrWitness.future,T> future(HashSet<Future<T>> type){
            return Nested.of(widen(type.map(FutureKind::widen)),Instances.definitions(),Futures.Instances.definitions());
        }
        public static <T> Nested<hashSet,queue,T>  queue(HashSet<Queue<T>> nested){
            return Nested.of(widen(nested.map(QueueKind::widen)),Instances.definitions(),Queues.Instances.definitions());
        }
        public static <L, R> Nested<hashSet,Higher<VavrWitness.either,L>, R> either(HashSet<Either<L, R>> nested){
            return Nested.of(widen(nested.map(EitherKind::widen)),Instances.definitions(),Eithers.Instances.definitions());
        }
        public static <T> Nested<hashSet,VavrWitness.stream,T> stream(HashSet<Stream<T>> nested){
            return Nested.of(widen(nested.map(StreamKind::widen)),Instances.definitions(),Streams.Instances.definitions());
        }
        public static <T> Nested<hashSet,VavrWitness.list,T> list(HashSet<List<T>> nested){
            return Nested.of(widen(nested.map(ListKind::widen)), Instances.definitions(),Lists.Instances.definitions());
        }
        public static <T> Nested<hashSet,array,T> array(HashSet<Array<T>> nested){
            return Nested.of(widen(nested.map(ArrayKind::widen)),Instances.definitions(),Arrays.Instances.definitions());
        }
        public static <T> Nested<hashSet,vector,T> vector(HashSet<Vector<T>> nested){
            return Nested.of(widen(nested.map(VectorKind::widen)),Instances.definitions(),Vectors.Instances.definitions());
        }
        public static <T> Nested<hashSet,hashSet,T> set(HashSet<HashSet<T>> nested){
            return Nested.of(widen(nested.map(HashSetKind::widen)),Instances.definitions(), HashSets.Instances.definitions());
        }

        public static <T> Nested<hashSet,reactiveSeq,T> reactiveSeq(HashSet<ReactiveSeq<T>> nested){
            HashSetKind<ReactiveSeq<T>> x = widen(nested);
            HashSetKind<Higher<reactiveSeq,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<hashSet,maybe,T> maybe(HashSet<Maybe<T>> nested){
            HashSetKind<Maybe<T>> x = widen(nested);
            HashSetKind<Higher<maybe,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<hashSet,eval,T> eval(HashSet<Eval<T>> nested){
            HashSetKind<Eval<T>> x = widen(nested);
            HashSetKind<Higher<eval,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<hashSet,Witness.future,T> cyclopsFuture(HashSet<cyclops.async.Future<T>> nested){
            HashSetKind<cyclops.async.Future<T>> x = widen(nested);
            HashSetKind<Higher<Witness.future,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<hashSet,Higher<Witness.either,S>, P> xor(HashSet<Either<S, P>> nested){
            HashSetKind<Either<S, P>> x = widen(nested);
            HashSetKind<Higher<Higher<Witness.either,S>, P>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),Either.Instances.definitions());
        }
        public static <S,T> Nested<hashSet,Higher<reader,S>, T> reader(HashSet<Reader<S, T>> nested,S defaultValue){
            HashSetKind<Reader<S, T>> x = widen(nested);
            HashSetKind<Higher<Higher<reader,S>, T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions(defaultValue));
        }
        public static <S extends Throwable, P> Nested<hashSet,Higher<Witness.tryType,S>, P> cyclopsTry(HashSet<cyclops.control.Try<P, S>> nested){
            HashSetKind<cyclops.control.Try<P, S>> x = widen(nested);
            HashSetKind<Higher<Higher<Witness.tryType,S>, P>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<hashSet,optional,T> setal(HashSet<Optional<T>> nested){
            HashSetKind<Optional<T>> x = widen(nested);
            HashSetKind<Higher<optional,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(), Optionals.Instances.definitions());
        }
        public static <T> Nested<hashSet,completableFuture,T> completableSet(HashSet<CompletableFuture<T>> nested){
            HashSetKind<CompletableFuture<T>> x = widen(nested);
            HashSetKind<Higher<completableFuture,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<hashSet,Witness.stream,T> javaStream(HashSet<java.util.stream.Stream<T>> nested){
            HashSetKind<java.util.stream.Stream<T>> x = widen(nested);
            HashSetKind<Higher<Witness.stream,T>> y = (HashSetKind)x;
            return Nested.of(y,Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }



    }
    public static interface NestedSet{
        public static <T> Nested<reactiveSeq,hashSet,T> reactiveSeq(ReactiveSeq<HashSet<T>> nested){
            ReactiveSeq<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);
            return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
        }

        public static <T> Nested<maybe,hashSet,T> maybe(Maybe<HashSet<T>> nested){
            Maybe<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);

            return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<eval,hashSet,T> eval(Eval<HashSet<T>> nested){
            Eval<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);

            return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.future,hashSet,T> cyclopsFuture(cyclops.async.Future<HashSet<T>> nested){
            cyclops.async.Future<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);

            return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
        }
        public static <S, P> Nested<Higher<Witness.either,S>,hashSet, P> xor(Either<S, HashSet<P>> nested){
            Either<S, Higher<hashSet,P>> x = nested.map(HashSetKind::widenK);

            return Nested.of(x,Either.Instances.definitions(),Instances.definitions());
        }
        public static <S,T> Nested<Higher<reader,S>,hashSet, T> reader(Reader<S, HashSet<T>> nested,S defaultValue){

            Reader<S, Higher<hashSet, T>>  x = nested.map(HashSetKind::widenK);

            return Nested.of(x,Reader.Instances.definitions(defaultValue),Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,hashSet, P> cyclopsTry(cyclops.control.Try<HashSet<P>, S> nested){
            cyclops.control.Try<Higher<hashSet,P>, S> x = nested.map(HashSetKind::widenK);

            return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<optional,hashSet,T> setal(Optional<HashSet<T>> nested){
            Optional<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);

            return  Nested.of(Optionals.OptionalKind.widen(x), Optionals.Instances.definitions(), Instances.definitions());
        }
        public static <T> Nested<completableFuture,hashSet,T> completableSet(CompletableFuture<HashSet<T>> nested){
            CompletableFuture<Higher<hashSet,T>> x = nested.thenApply(HashSetKind::widenK);

            return Nested.of(CompletableFutures.CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.stream,hashSet,T> javaStream(java.util.stream.Stream<HashSet<T>> nested){
            java.util.stream.Stream<Higher<hashSet,T>> x = nested.map(HashSetKind::widenK);

            return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
        }
    }



}
