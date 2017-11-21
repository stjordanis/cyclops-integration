package com.aol.cyclops.vavr.hkt;

import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.hkt.Higher2;
import com.oath.cyclops.types.foldable.To;
import cyclops.companion.vavr.Eithers;
import cyclops.conversion.vavr.ToCyclopsReact;
import cyclops.monads.VavrWitness.either;
import cyclops.monads.WitnessType;
import cyclops.monads.transformers.EitherT;
import cyclops.typeclasses.Active;
import cyclops.typeclasses.InstanceDefinitions;
import cyclops.typeclasses.Nested;

import io.vavr.control.Either;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.function.*;


public interface EitherKind<L,R> extends Either<L,R>,To<EitherKind<L,R>>,
                                        Higher2<either,L,R> {

    default <R2> EitherKind<L,R2> fold(Function<? super Either<? super L,? super R>,? extends Either<L,R2>> op){
        return widen(op.apply(this));
    }
    public static <L,R> Higher<Higher<either,L>,R> widenK(final Either<L,R> completableList) {

        return new EitherKind.Box<>(
                completableList);
    }
    public static <L,R> EitherKind<L,R> widen(final Either<L,R> either) {
        return new Box<>(either);
    }
    public static <L,R> EitherKind<L,R> rightK(final R right) {
        return new Box<>(Either.right(right));
    }
    public static <L,R> EitherKind<L,R> leftK(final L left) {
        return new Box<>(Either.left(left));
    }
    public static <L, R> Either<L, R> narrowK2(final Higher2<either, L, R> xor) {
        return (Either<L, R>)xor;
    }
    public static <L, R> Either<L, R> narrowK(final Higher<Higher<either, L>, R> either) {
        return (Either<L, R>)either;
    }
    default Active<Higher<either,L>,R> allTypeclasses(){
        return Active.of(this, Eithers.Instances.definitions());
    }
    default <W2,R2> Nested<Higher<either,L>,W2,R2> mapM(Function<? super R,? extends Higher<W2,R2>> fn, InstanceDefinitions<W2> defs){
        return Nested.of(widen(bimap(l->l,r->fn.apply(r))), Eithers.Instances.definitions(), defs);
    }
    default <W extends WitnessType<W>> EitherT<W, L,R> liftM(W witness) {
        return EitherT.of(witness.adapter().unit(ToCyclopsReact.either(this)));
    }
    public static <L1, R1> Either<L1, R1> right(R1 right) {
        return Either.right(right);
    }

    public static <L1, R1> Either<L1, R1> left(L1 left) {
        return Either.left(left);
    }

    public static <L1, R1> Either<L1, R1> narrow(Either<? extends L1, ? extends R1> either) {
        return Either.narrow(either);
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Box<L,R> implements EitherKind<L,R> {

        private final Either<L,R> boxed;

        public Either<L,R> narrow() {
            return boxed;
        }

        @Override
        public L getLeft() {
            return boxed.getLeft();
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public boolean isRight() {
            return boxed.isRight();
        }

        @Override
        public R get() {
            return boxed.get();
        }

        @Override
        public String stringPrefix() {
            return boxed.stringPrefix();
        }
    }




}
