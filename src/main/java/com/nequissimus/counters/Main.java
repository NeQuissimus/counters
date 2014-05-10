package com.nequissimus.counter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
* $ sysctl -n machdep.cpu.brand_string
*  Intel(R) Core(TM) i7-3770S CPU @ 3.10GHz
*
* Counts per second (CPS):
*    LongAdder:     199,251,313
*    Synchronized:   27,316,974
*    AtomicLong:     60,482,771
*/

public final class Main {
  private static final long MAX = 100_000_000;

  private static final LongAdder COUNTER_ADDER = new LongAdder();
  private static final Runnable RUNNABLE_ADDER = new Runnable() {
      @Override public void run() { for (long i = 0; i < MAX; i++) { COUNTER_ADDER.increment(); } }
    };

  private static long counter_sync = 0L;
  private static final Object LOCK_SYNC = new Object();
  private static final Runnable RUNNABLE_SYNC = new Runnable() {
    @Override public void run() { for (long i = 0; i < MAX; i++) { synchronized(LOCK_SYNC) { counter_sync++; } } }
  };

  private static final AtomicLong COUNTER_ATOMIC = new AtomicLong();
  private static final Runnable RUNNABLE_ATOMIC = new Runnable() {
    @Override public void run() { for (long i = 0; i < MAX; i++) { COUNTER_ATOMIC.getAndIncrement(); } }
  };

  private static final Runnable TASK_TO_RUN = RUNNABLE_ADDER; // Change me!

  public static void main(String[] args) throws Exception {
    final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, Runtime.getRuntime().availableProcessors(), 10, TimeUnit.SECONDS, workQueue);
    final long startT = System.nanoTime();
    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      executor.execute(TASK_TO_RUN);
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES);

    final long endT = System.nanoTime();
    final long deltaT = endT - startT;

    System.out.println(String.format("Time in ns: %d", deltaT));
    if (TASK_TO_RUN == RUNNABLE_ADDER) {
      System.out.println("=======================");
      System.out.println("LongAdder");
      System.out.println(String.format("Count: %d", COUNTER_ADDER.sum()));
      System.out.println(String.format("Counts per second: %f", (double)COUNTER_ADDER.sum() / deltaT * 1_000_000_000));
    } else if (TASK_TO_RUN == RUNNABLE_SYNC) {
      System.out.println("=======================");
      System.out.println("synchronized");
      System.out.println(String.format("Count: %d", counter_sync));
      System.out.println(String.format("Counts per second: %f", (double)counter_sync / deltaT * 1_000_000_000));
    } else if (TASK_TO_RUN == RUNNABLE_ATOMIC) {
      System.out.println("=======================");
      System.out.println("AtomicLong");
      System.out.println(String.format("Count: %d", COUNTER_ATOMIC.get()));
      System.out.println(String.format("Counts per second: %f", (double)COUNTER_ATOMIC.get() / deltaT * 1_000_000_000));
    }

    executor.shutdownNow();
  }
}
