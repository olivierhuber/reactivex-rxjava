package com.zenika.reactivex;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class RxJavaTutorial {

    public static void main(String[] args) {
        step00ImperativeGreeting();
//        step01JustHello();
//        step02HelloWorld();
//        step03CreateObservable();
//        step04Schedulers();
//        step05Operators();
    }

    public static void step00ImperativeGreeting() {
        String greeting = "Hello world!";

        System.out.println(greeting);

        // Console:
        // Hello world!
    }

    public static void step01JustHello() {
        Observable<String> greetingObservable = Observable.just("Hello");

        greetingObservable.subscribe(System.out::println);

        // Console:
        // Hello
    }

    public static void step02HelloWorld() {
        Observable<String> greetingObservable = Observable.from(Arrays.asList("Hello", "World!"));

        greetingObservable
                .doOnSubscribe(() -> System.out.println("OnSubscribe"))
                .doOnError(error -> System.err.println("OnError:" + error.getMessage()))
                .doOnNext(value -> System.out.println("OnNext:" + value))
                .doOnCompleted(() -> System.out.println("OnCompleted"))
                .subscribe();

        // Console:
        // OnSubscribe
        // OnNext:Hello
        // OnNext:World!
        // OnCompleted

    }

    public static void step03CreateObservable() {
        Observable<String> greetingObservable = Observable.create(subscriber -> {
            System.out.println("Observable.create()");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Rx Contract http://reactivex.io/documentation/contract.html
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext("Hello");
                subscriber.onNext("World!");
                subscriber.onCompleted();
            }
        });

        greetingObservable
                .doOnSubscribe(() -> System.out.println("OnSubscribe"))
                .doOnError(error -> System.err.println("OnError:" + error.getMessage()))
                .doOnNext(value -> System.out.println("OnNext:" + value))
                .doOnCompleted(() -> System.out.println("OnCompleted"))
                .subscribe();

        // Console:
        // OnSubscribe
        // Observable.create()
        // OnNext:Hello
        // OnNext:World!
        // OnCompleted
    }

    public static void step04Schedulers() {
        Observable<String> greetingObservable = Observable.create(subscriber -> {
            System.out.println(Thread.currentThread().getName() + ":Observable.create()");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext("Hello");
                subscriber.onNext("World!");
            }
            subscriber.onCompleted();
        });

        Subscription subscription = greetingObservable
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(() -> System.out.println(Thread.currentThread().getName() + ":OnSubscribe"))
                .doOnError(error -> System.err.println(Thread.currentThread().getName() + ":OnError:" + error.getMessage()))
                .observeOn(Schedulers.io())
                .doOnNext(value -> System.out.println(Thread.currentThread().getName() + ":OnNext:" + value))
                .observeOn(Schedulers.newThread())
                .map(String::length)
                .doOnNext(value -> System.out.println(Thread.currentThread().getName() + ":OnNext(length):" + value))
                .doOnCompleted(() -> System.out.println(Thread.currentThread().getName() + ":OnCompleted"))
                .doOnUnsubscribe(() -> System.out.println(Thread.currentThread().getName() + ":OnUnsubscribe"))
                .subscribe();

        System.out.println(Thread.currentThread().getName() + ":Right after the subscription");

        try {
            TimeUnit.SECONDS.sleep(2);
            subscription.unsubscribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Console:
        // main:OnSubscribe
        // RxComputationScheduler-1:Observable.create()
        // main:Right after the subscription
        // RxIoScheduler-2:OnNext:Hello
        // RxIoScheduler-2:OnNext:World!
        // RxNewThreadScheduler-1:OnNext(length):5
        // RxNewThreadScheduler-1:OnNext(length):6
        // RxNewThreadScheduler-1:OnCompleted
        // RxNewThreadScheduler-1:OnUnsubscribe
    }

    public static void step05Operators() {
        Observable<String> infinite = Observable.interval(300, TimeUnit.MILLISECONDS).map(Object::toString);
        Observable<String> ticks = Observable.interval(1, TimeUnit.SECONDS).map(i -> "TICK " + (i + 1)).startWith("START");

        Subscription subscription = infinite.mergeWith(ticks).subscribe(System.out::println);
        try {
            TimeUnit.SECONDS.sleep(3);
            subscription.unsubscribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Console:
        // START
        // 0
        // 1
        // 2
        // TICK 1
        // 3
        // 4
        // 5
        // TICK 2
        // 6
        // 7
        // 8
        // 9
        // TICK 3
    }
}
