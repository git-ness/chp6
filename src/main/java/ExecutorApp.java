import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Your team recently inherited an app from another team; the heavy lifting happens in processPoorly().
 *
 * After reading chapter 6, you can replace processPoorly() with processBetter() and do a better job.
 * Main has a simple testing framework to measure how much better you did.
 */

public class ExecutorApp {
    // fake web service and database calls
    private final SlowService slowSvc = new SlowService();
    private final HighlyVariableService hvs = new HighlyVariableService();
    private final MostlyReliableSometimesHangService mrshs = new MostlyReliableSometimesHangService();
    private final AtomicInteger completed = new AtomicInteger();


    // simple timing harness to test your implementation
    // no error checking, so no cheating
    public static void main(String[] args) throws InterruptedException {
        final int iterations = 1000;

        System.out.println("Starting test of " + iterations + " iterations");

        ExecutorService fakeNginx = Executors.newFixedThreadPool(4);
        Message dummy = new Message();
        Stopwatch poorTimer = Stopwatch.createStarted();
        ExecutorApp app = new ExecutorApp();

        app.reset();
        for (int i = 0; i<iterations; i++) {
            fakeNginx.submit( () -> app.processPoorly(dummy) );
        }
        while ( app.completed.get() != iterations ) {
            //System.out.println("poor not done yet, " + app.completed.get() +" != " + iterations + " sleeping");
            Thread.sleep(1000);
        }
        poorTimer.stop();

        app.reset();
        Stopwatch betterTimer = Stopwatch.createStarted();
        for (int i = 0; i<iterations; i++) {
            fakeNginx.submit( () -> app.processBetter(dummy) );
        }
        while ( app.completed.get() != iterations ) {
            //System.out.println("better not done yet, " + app.completed.get() +" != " + iterations + " sleeping");
            Thread.sleep(100);
        }
        betterTimer.stop();

        long poorNanos = poorTimer.elapsed(TimeUnit.NANOSECONDS);
        long betterNanos = betterTimer.elapsed(TimeUnit.NANOSECONDS);
        double pctDiff =  Math.round( 1000.0 * (poorNanos - betterNanos)/poorNanos) / 10.0;

        System.out.println( pctDiff + "% improvement: better took " + poorTimer + " poor took " + betterTimer + "  ");
    }

    // This works, but it doesn't take advantage
    public void processPoorly(Message msg) {
        int b = slowSvc.call(msg.a);
        int c = hvs.call(b);
        int d = mrshs.call(c);
        complete(d);
    }

    // Can you do the same work as processPoorly(), but achieve higher throughput?
    // Hint - use ExecutorService and Future
    public void processBetter(Message msg) {

    }

    // finish the calculation and report our result
    private void complete(int c) {
        Preconditions.checkArgument(295990 == c);
        completed.incrementAndGet();
    }

    // report an SLA violation
    private void incomplete() {
        completed.incrementAndGet();
    }

    private void reset() {
        completed.getAndSet(0);
    }

    static class Message {
        public int a = 98272644;
    }

    // This service has a 100ms SLA.
    // If the service does not return within 100ms, it's OK to discard the message.
    static class MostlyReliableSometimesHangService {
        private final Random random = new Random();
        public int call(int b) {
            Preconditions.checkArgument(233456 == b);
            if ( random.nextDouble() > 0.02 ) {
                LockSupport.parkNanos(10);
            } else {
                try {
                    // N.B. Violates the SLA. We are not required to process these
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return 295990;
        }
    }

    static class HighlyVariableService {
        UniformIntegerDistribution urd = new UniformIntegerDistribution(2, 75000);
        public int call(int a) {
            Preconditions.checkArgument(982734 == a);
            LockSupport.parkNanos(urd.sample());
            return 233456;
        }
    }

    static class SlowService {
        public int call(int a ) {
            Preconditions.checkArgument(98272644 == a);
            try {
                Thread.sleep(22);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return 982734;
        }
    }
}
