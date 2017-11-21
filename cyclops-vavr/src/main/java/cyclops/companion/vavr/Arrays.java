package cyclops.companion.vavr;


import com.aol.cyclops.vavr.hkt.*;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Optionals;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Reader;
import cyclops.control.Either;
import cyclops.conversion.vavr.FromCyclopsReact;
import cyclops.monads.*;
import cyclops.monads.VavrWitness.*;
import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.types.anyM.AnyMSeq;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;

import cyclops.monads.VavrWitness.either;
import cyclops.monads.VavrWitness.future;
import cyclops.monads.VavrWitness.list;
import cyclops.monads.VavrWitness.tryType;
import cyclops.monads.Witness.*;
import cyclops.reactive.ReactiveSeq;
import cyclops.typeclasses.*;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.Lazy;
import io.vavr.collection.*;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import cyclops.data.tuple.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static com.aol.cyclops.vavr.hkt.ArrayKind.narrowK;
import static com.aol.cyclops.vavr.hkt.ArrayKind.widen;


public class Arrays {

    public static  <W1,T> Coproduct<W1,array,T> coproduct(Array<T> list, InstanceDefinitions<W1> def1){
        return Coproduct.of(cyclops.control.Either.right(widen(list)),def1, Instances.definitions());
    }
    public static  <W1,T> Coproduct<W1,array,T> coproduct(InstanceDefinitions<W1> def1,T... values){
        return coproduct(Array.of(values),def1);
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,array,T> xorM(Array<T> type){
        return XorM.right(anyM(type));
    }

    public static <T> AnyMSeq<array,T> anyM(Array<T> option) {
        return AnyM.ofSeq(option, array.INSTANCE);
    }

    public static  <T,R> Array<R> tailRec(T initial, Function<? super T, ? extends Array<? extends io.vavr.control.Either<T, R>>> fn) {
        Array<io.vavr.control.Either<T, R>> next = Array.of(io.vavr.control.Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.flatMap(e -> e.fold(s -> {
                        newValue[0]=true;
                        return fn.apply(s); },
                    p -> {
                        newValue[0]=false;
                        return Array.of(e);
                    }));
            if(!newValue[0])
                break;

        }

        return next.filter(io.vavr.control.Either::isRight).map(io.vavr.control.Either::get);
    }
    public static  <T,R> Array<R> tailRecEither(T initial, Function<? super T, ? extends Array<? extends cyclops.control.Either<T, R>>> fn) {
        Array<cyclops.control.Either<T, R>> next = Array.of(cyclops.control.Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.flatMap(e -> e.visit(s -> {
                        newValue[0]=true;
                        return fn.apply(s); },
                    p -> {
                        newValue[0]=false;
                        return Array.of(e);
                    }));
            if(!newValue[0])
                break;

        }

        return next.filter(cyclops.control.Either::isRight).map(e->e.orElse(null));
    }


    /**
     * Perform a For Comprehension over a Array, accepting 3 generating functions.
     * This results in a four level nested internal iteration over the provided Publishers.
     *
     *  <pre>
     * {@code
     *
     *   import static cyclops.Arrays.forEach4;
     *
    forEach4(IntArray.range(1,10).boxed(),
    a-> Array.iterate(a,i->i+1).limit(10),
    (a,b) -> Array.<Integer>of(a+b),
    (a,b,c) -> Array.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Array
     * @param value2 Nested Array
     * @param value3 Nested Array
     * @param value4 Nested Array
     * @param yieldingFunction  Generates a result per combination
     * @return Array with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Array<R> forEach4(Array<? extends T1> value1,
                                                               Function<? super T1, ? extends Array<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends Array<R2>> value3,
                                                               Function3<? super T1, ? super R1, ? super R2, ? extends Array<R3>> value4,
                                                               Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Array<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Array<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Array, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     *  import static com.oath.cyclops.reactor.Arrayes.forEach4;
     *
     *  forEach4(IntArray.range(1,10).boxed(),
    a-> Array.iterate(a,i->i+1).limit(10),
    (a,b) -> Array.<Integer>just(a+b),
    (a,b,c) -> Array.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Array
     * @param value2 Nested Array
     * @param value3 Nested Array
     * @param value4 Nested Array
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Array with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Array<R> forEach4(Array<? extends T1> value1,
                                                                 Function<? super T1, ? extends Array<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Array<R2>> value3,
                                                                 Function3<? super T1, ? super R1, ? super R2, ? extends Array<R3>> value4,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Function4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Array<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Array<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }

    /**
     * Perform a For Comprehension over a Array, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     * import static Arrays.forEach3;
     *
     * forEach(IntArray.range(1,10).boxed(),
    a-> Array.iterate(a,i->i+1).limit(10),
    (a,b) -> Array.<Integer>of(a+b),
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     *
     * @param value1 top level Array
     * @param value2 Nested Array
     * @param value3 Nested Array
     * @param yieldingFunction Generates a result per combination
     * @return Array with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Array<R> forEach3(Array<? extends T1> value1,
                                                         Function<? super T1, ? extends Array<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Array<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Array<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });


    }

    /**
     * Perform a For Comprehension over a Array, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     * import static Arrays.forEach;
     *
     * forEach(IntArray.range(1,10).boxed(),
    a-> Array.iterate(a,i->i+1).limit(10),
    (a,b) -> Array.<Integer>of(a+b),
    (a,b,c) ->a+b+c<10,
    Tuple::tuple)
    .toArrayX();
     * }
     * </pre>
     *
     * @param value1 top level Array
     * @param value2 Nested publisher
     * @param value3 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T1, T2, R1, R2, R> Array<R> forEach3(Array<? extends T1> value1,
                                                         Function<? super T1, ? extends Array<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Array<R2>> value3,
                                                         Function3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Function3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Array<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });
    }

    /**
     * Perform a For Comprehension over a Array, accepting an additonal generating function.
     * This results in a two level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     *  import static Arrays.forEach2;
     *  forEach(IntArray.range(1, 10).boxed(),
     *          i -> Array.range(i, 10), Tuple::tuple)
    .forEach(System.out::println);

    //(1, 1)
    (1, 2)
    (1, 3)
    (1, 4)
    ...
     *
     * }</pre>
     *
     * @param value1 top level Array
     * @param value2 Nested publisher
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Array<R> forEach2(Array<? extends T> value1,
                                                Function<? super T, Array<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });

    }

    /**
     *
     * <pre>
     * {@code
     *
     *   import static Arrays.forEach2;
     *
     *   forEach(IntArray.range(1, 10).boxed(),
     *           i -> Array.range(i, 10),
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
     * @param value1 top level Array
     * @param value2 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Array<R> forEach2(Array<? extends T> value1,
                                                Function<? super T, ? extends Array<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Array<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });
    }
    public static <T> Active<array,T> allTypeclasses(Array<T> array){
        return Active.of(widen(array), Instances.definitions());
    }
    public static <T,W2,R> Nested<array,W2,R> mapM(Array<T> array, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        Array<Higher<W2, R>> e = array.map(fn);
        ArrayKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, Arrays.Instances.definitions(), defs);
    }
    /**
     * Companion class for creating Type Class instances for working with Arrays*
     */
    @UtilityClass
    public static class Instances {
        public static InstanceDefinitions<array> definitions() {
            return new InstanceDefinitions<array>() {

                @Override
                public <T, R> Functor<array> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<array> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<array> applicative() {
                    return Instances.zippingApplicative();
                }

                @Override
                public <T, R> Monad<array> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<array>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<array>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> MonadRec<array> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<array>> monadPlus(Monoid<Higher<array, T>> m) {
                    return Maybe.just(Instances.monadPlus(m));
                }

                @Override
                public <C2, T>  Traverse<array> traverse() {
                    return Instances.traverse();
                }

                @Override
                public <T> Foldable<array> foldable() {
                    return Instances.foldable();
                }

                @Override
                public <T> Maybe<Comonad<array>> comonad() {
                    return Maybe.nothing();
                }

                @Override
                public <T> Maybe<Unfoldable<array>> unfoldable() {
                    return Maybe.just(Instances.unfoldable());
                }
            };

        }
        /**
         *
         * Transform a list, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  ArrayKind<Integer> list = Arrays.functor().map(i->i*2, ArrayKind.widen(Arrays.asArray(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Arrays
         * <pre>
         * {@code
         *   ArrayKind<Integer> list = Arrays.unit()
        .unit("hello")
        .then(h->Arrays.functor().map((String v) ->v.length(), h))
        .convert(ArrayKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Arrays
         */
        public static <T,R>Functor<array> functor(){
            BiFunction<ArrayKind<T>,Function<? super T, ? extends R>,ArrayKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * ArrayKind<String> list = Arrays.unit()
        .unit("hello")
        .convert(ArrayKind::narrowK);

        //Arrays.asArray("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Arrays
         */
        public static <T> Pure<array> unit(){
            return General.<array,T>unit(ArrayKind::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.ArrayKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Arrays.asArray;
         *
        Arrays.zippingApplicative()
        .ap(widen(asArray(l1(this::multiplyByTwo))),widen(asArray(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * ArrayKind<Function<Integer,Integer>> listFn =Arrays.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(ArrayKind::narrowK);

        ArrayKind<Integer> list = Arrays.unit()
        .unit("hello")
        .then(h->Arrays.functor().map((String v) ->v.length(), h))
        .then(h->Arrays.zippingApplicative().ap(listFn, h))
        .convert(ArrayKind::narrowK);

        //Arrays.asArray("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Arrays
         */
        public static <T,R> Applicative<array> zippingApplicative(){
            BiFunction<ArrayKind< Function<T, R>>,ArrayKind<T>,ArrayKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.ArrayKind.widen;
         * ArrayKind<Integer> list  = Arrays.monad()
        .flatMap(i->widen(ArrayX.range(0,i)), widen(Arrays.asArray(1,2,3)))
        .convert(ArrayKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    ArrayKind<Integer> list = Arrays.unit()
        .unit("hello")
        .then(h->Arrays.monad().flatMap((String v) ->Arrays.unit().unit(v.length()), h))
        .convert(ArrayKind::narrowK);

        //Arrays.asArray("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Arrays
         */
        public static <T,R> Monad<array> monad(){

            BiFunction<Higher<array,T>,Function<? super T, ? extends Higher<array,R>>,Higher<array,R>> flatMap = Instances::flatMap;
            return General.monad(zippingApplicative(), flatMap);
        }

        public static <T> MonadRec<array> monadRec(){
            return new MonadRec<array>(){

                @Override
                public <T, R> Higher<array, R> tailRec(T initial, Function<? super T, ? extends Higher<array, ? extends cyclops.control.Either<T, R>>> fn) {
                    return widen(tailRecEither(initial,fn.andThen(ArrayKind::narrowK).andThen(a->a.narrow())));
                }
            };
        }
        /**
         *
         * <pre>
         * {@code
         *  ArrayKind<String> list = Arrays.unit()
        .unit("hello")
        .then(h->Arrays.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(ArrayKind::narrowK);

        //Arrays.asArray("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<array> monadZero(){

            return General.monadZero(monad(), widen(Array.empty()));
        }
        /**
         * <pre>
         * {@code
         *  ArrayKind<Integer> list = Arrays.<Integer>monadPlus()
        .plus(ArrayKind.widen(Arrays.asArray()), ArrayKind.widen(Arrays.asArray(10)))
        .convert(ArrayKind::narrowK);
        //Arrays.asArray(10))
         *
         * }
         * </pre>
         * @return Type class for combining Arrays by concatenation
         */
        public static <T> MonadPlus<array> monadPlus(){
            Monoid<ArrayKind<T>> m = Monoid.of(widen(Array.empty()), Instances::concat);
            Monoid<Higher<array,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<ArrayKind<Integer>> m = Monoid.of(ArrayKind.widen(Arrays.asArray()), (a,b)->a.isEmpty() ? b : a);
        ArrayKind<Integer> list = Arrays.<Integer>monadPlus(m)
        .plus(ArrayKind.widen(Arrays.asArray(5)), ArrayKind.widen(Arrays.asArray(10)))
        .convert(ArrayKind::narrowK);
        //Arrays.asArray(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining Arrays
         * @return Type class for combining Arrays
         */
        public static <T> MonadPlus<array> monadPlus(Monoid<Higher<array, T>> m){
            Monoid<Higher<array,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        public static <T> MonadPlus<array> monadPlusK(Monoid<ArrayKind<T>> m){
            Monoid<Higher<array,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        public static Unfoldable<array> unfoldable(){
            return new Unfoldable<array>() {
                @Override
                public <R, T> Higher<array, R> unfold(T b, Function<? super T, cyclops.control.Option<Tuple2<R, T>>> fn) {
                    return widen(ReactiveSeq.unfold(b,fn).collect(Array.collector()));

                }
            };
        }
        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<array> traverse(){

            BiFunction<Applicative<C2>,ArrayKind<Higher<C2, T>>,Higher<C2, ArrayKind<T>>> sequenceFn = (ap, list) -> {

                Higher<C2,ArrayKind<T>> identity = ap.unit(widen(Array.empty()));

                BiFunction<Higher<C2,ArrayKind<T>>,Higher<C2,T>,Higher<C2,ArrayKind<T>>> combineToArray =   (acc, next) -> ap.apBiFn(ap.unit((a, b) -> widen(ArrayKind.narrow(a).append(b))),
                        acc,next);

                BinaryOperator<Higher<C2,ArrayKind<T>>> combineArrays = (a, b)-> ap.apBiFn(ap.unit((l1, l2)-> widen(ArrayKind.narrow(l1).appendAll(l2.narrow()))),a,b); ;

                return ReactiveSeq.fromIterable(ArrayKind.narrow(list))
                        .reduce(identity,
                                combineToArray,
                                combineArrays);


            };
            BiFunction<Applicative<C2>,Higher<array,Higher<C2, T>>,Higher<C2, Higher<array,T>>> sequenceNarrow  =
                    (a,b) -> ArrayKind.widen2(sequenceFn.apply(a, narrowK(b)));
            return General.traverse(zippingApplicative(), sequenceNarrow);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Arrays.foldable()
        .foldLeft(0, (a,b)->a+b, ArrayKind.widen(Arrays.asArray(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<array> foldable(){
            return new Foldable<array>() {
                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<array, T> ds) {
                    return narrowK(ds).narrow().foldRight(monoid.zero(),monoid);
                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<array, T> ds) {
                    return narrowK(ds).narrow().foldLeft(monoid.zero(),monoid);
                }

                @Override
                public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<array, T> nestedA) {
                    return narrowK(nestedA).narrow().foldRight(mb.zero(),(a,b)->mb.apply(fn.apply(a),b));
                }
            };
        }

        private static  <T> ArrayKind<T> concat(ArrayKind<T> l1, ArrayKind<T> l2){

            return widen(l1.appendAll(ArrayKind.narrow(l2)));

        }

        private static <T,R> ArrayKind<R> ap(ArrayKind<Function< T, R>> lt, ArrayKind<T> list){
            return widen(FromCyclopsReact.fromStream(ReactiveSeq.fromIterable(lt.narrow()).zip(list.narrow(), (a, b)->a.apply(b))).toArray());
        }
        private static <T,R> Higher<array,R> flatMap(Higher<array,T> lt, Function<? super T, ? extends  Higher<array,R>> fn){
            return widen(ArrayKind.narrow(lt).flatMap(fn.andThen(ArrayKind::narrow)));
        }
        private static <T,R> ArrayKind<R> map(ArrayKind<T> lt, Function<? super T, ? extends R> fn){
            return widen(ArrayKind.narrow(lt).map(in->fn.apply(in)));
        }
    }

    public static interface ArrayNested{


        public static <T> Nested<array,option,T> option(Array<Option<T>> type){
            return Nested.of(widen(type.map(OptionKind::widen)),Instances.definitions(),Options.Instances.definitions());
        }
        public static <T> Nested<array,tryType,T> arrayTry(Array<Try<T>> type){
            return Nested.of(widen(type.map(TryKind::widen)),Instances.definitions(),Trys.Instances.definitions());
        }
        public static <T> Nested<array,future,T> future(Array<Future<T>> type){
            return Nested.of(widen(type.map(FutureKind::widen)),Instances.definitions(),Futures.Instances.definitions());
        }
        public static <T> Nested<array,lazy,T> lazy(Array<Lazy<T>> nested){
            return Nested.of(widen(nested.map(LazyKind::widen)),Instances.definitions(),Lazys.Instances.definitions());
        }
        public static <L, R> Nested<array,Higher<either,L>, R> either(Array<io.vavr.control.Either<L, R>> nested){
            return Nested.of(widen(nested.map(EitherKind::widen)),Instances.definitions(),Eithers.Instances.definitions());
        }
        public static <T> Nested<array,VavrWitness.queue,T> queue(Array<Queue<T>> nested){
            return Nested.of(widen(nested.map(QueueKind::widen)), Instances.definitions(),Queues.Instances.definitions());
        }
        public static <T> Nested<array,VavrWitness.stream,T> stream(Array<Stream<T>> nested){
            return Nested.of(widen(nested.map(StreamKind::widen)),Instances.definitions(),Streams.Instances.definitions());
        }
        public static <T> Nested<array,list,T> list(Array<List<T>> nested){
            return Nested.of(widen(nested.map(ListKind::widen)), Instances.definitions(),Lists.Instances.definitions());
        }
        public static <T> Nested<array,array,T> array(Array<Array<T>> nested){
            return Nested.of(widen(nested.map(ArrayKind::widen)),Instances.definitions(),Arrays.Instances.definitions());
        }
        public static <T> Nested<array,vector,T> vector(Array<Vector<T>> nested){
            return Nested.of(widen(nested.map(VectorKind::widen)),Instances.definitions(),Vectors.Instances.definitions());
        }
        public static <T> Nested<array,hashSet,T> set(Array<HashSet<T>> nested){
            return Nested.of(widen(nested.map(HashSetKind::widen)),Instances.definitions(), HashSets.Instances.definitions());
        }

        public static <T> Nested<array,reactiveSeq,T> reactiveSeq(Array<ReactiveSeq<T>> nested){
            ArrayKind<ReactiveSeq<T>> x = widen(nested);
            ArrayKind<Higher<reactiveSeq,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<array,maybe,T> maybe(Array<Maybe<T>> nested){
            ArrayKind<Maybe<T>> x = widen(nested);
            ArrayKind<Higher<maybe,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<array,eval,T> eval(Array<Eval<T>> nested){
            ArrayKind<Eval<T>> x = widen(nested);
            ArrayKind<Higher<eval,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<array,Witness.future,T> cyclopsFuture(Array<cyclops.async.Future<T>> nested){
            ArrayKind<cyclops.async.Future<T>> x = widen(nested);
            ArrayKind<Higher<Witness.future,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<array,Higher<Witness.either,S>, P> xor(Array<cyclops.control.Either<S, P>> nested){
            ArrayKind<cyclops.control.Either<S, P>> x = widen(nested);
            ArrayKind<Higher<Higher<Witness.either,S>, P>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Either.Instances.definitions());
        }
        public static <S,T> Nested<array,Higher<reader,S>, T> reader(Array<Reader<S, T>> nested, S defaultValue){
            ArrayKind<Reader<S, T>> x = widen(nested);
            ArrayKind<Higher<Higher<reader,S>, T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions(defaultValue));
        }
        public static <S extends Throwable, P> Nested<array,Higher<Witness.tryType,S>, P> cyclopsTry(Array<cyclops.control.Try<P, S>> nested){
            ArrayKind<cyclops.control.Try<P, S>> x = widen(nested);
            ArrayKind<Higher<Higher<Witness.tryType,S>, P>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<array,optional,T> optional(Array<Optional<T>> nested){
            ArrayKind<Optional<T>> x = widen(nested);
            ArrayKind<Higher<optional,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(), Optionals.Instances.definitions());
        }
        public static <T> Nested<array,completableFuture,T> completableFuture(Array<CompletableFuture<T>> nested){
            ArrayKind<CompletableFuture<T>> x = widen(nested);
            ArrayKind<Higher<completableFuture,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<array,Witness.stream,T> javaStream(Array<java.util.stream.Stream<T>> nested){
            ArrayKind<java.util.stream.Stream<T>> x = widen(nested);
            ArrayKind<Higher<Witness.stream,T>> y = (ArrayKind)x;
            return Nested.of(y,Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }



    }
    public static interface NestedArray{
        public static <T> Nested<reactiveSeq,array,T> reactiveSeq(ReactiveSeq<Array<T>> nested){
            ReactiveSeq<Higher<array,T>> x = nested.map(ArrayKind::widenK);
            return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
        }

        public static <T> Nested<maybe,array,T> maybe(Maybe<Array<T>> nested){
            Maybe<Higher<array,T>> x = nested.map(ArrayKind::widenK);

            return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<eval,array,T> eval(Eval<Array<T>> nested){
            Eval<Higher<array,T>> x = nested.map(ArrayKind::widenK);

            return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.future,array,T> cyclopsFuture(cyclops.async.Future<Array<T>> nested){
            cyclops.async.Future<Higher<array,T>> x = nested.map(ArrayKind::widenK);

            return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
        }
        public static <S, P> Nested<Higher<Witness.either,S>,array, P> xor(cyclops.control.Either<S, Array<P>> nested){
          cyclops.control.Either<S, Higher<array,P>> x = nested.map(ArrayKind::widenK);

            return Nested.of(x,cyclops.control.Either.Instances.definitions(),Instances.definitions());
        }
        public static <S,T> Nested<Higher<reader,S>,array, T> reader(Reader<S, Array<T>> nested, S defaultValue){

            Reader<S, Higher<array, T>>  x = nested.map(ArrayKind::widenK);

            return Nested.of(x,Reader.Instances.definitions(defaultValue),Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,array, P> cyclopsTry(cyclops.control.Try<Array<P>, S> nested){
            cyclops.control.Try<Higher<array,P>, S> x = nested.map(ArrayKind::widenK);

            return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<optional,array,T> optional(Optional<Array<T>> nested){
            Optional<Higher<array,T>> x = nested.map(ArrayKind::widenK);

            return  Nested.of(Optionals.OptionalKind.widen(x), Optionals.Instances.definitions(), Instances.definitions());
        }
        public static <T> Nested<completableFuture,array,T> completableFuture(CompletableFuture<Array<T>> nested){
            CompletableFuture<Higher<array,T>> x = nested.thenApply(ArrayKind::widenK);

            return Nested.of(CompletableFutures.CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.stream,array,T> javaStream(java.util.stream.Stream<Array<T>> nested){
            java.util.stream.Stream<Higher<array,T>> x = nested.map(ArrayKind::widenK);

            return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
        }
    }



}
