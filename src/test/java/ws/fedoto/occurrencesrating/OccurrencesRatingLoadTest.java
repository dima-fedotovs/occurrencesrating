/*
 * Copyright (c) 2013 Dimitrijs Fedotovs.
 *
 * This file is part of OccurrencesRating library.
 *
 * OccurrencesRating library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OccurrencesRating library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OccurrencesRating library.  If not, see <http://www.gnu.org/licenses/>.
 */

package ws.fedoto.occurrencesrating;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public abstract class OccurrencesRatingLoadTest {
    private static final Random rand = new Random();
    private static final String TERMINATOR = "TERMINATE";

    protected abstract OccurrencesRating<String> createNewRating(int capacity);

    @Test
    public void test01Threads_10000_10000() throws Exception {
        loadTest(1, 10000, 10000);
    }

    @Test
    public void test01Threads_10000_15000() throws Exception {
        loadTest(1, 10000, 15000);
    }

    @Test
    public void test01Threads_100_20000() throws Exception {
        loadTest(1, 100, 20000);
    }

    @Test
    public void test01Threads_10000_2000() throws Exception {
        loadTest(1, 10000, 2000);
    }

    @Test
    public void test04Threads_10000_10000() throws Exception {
        loadTest(4, 10000, 10000);
    }

    @Test
    public void test04Threads_10000_15000() throws Exception {
        loadTest(4, 10000, 15000);
    }

    @Test
    public void test04Threads_100_20000() throws Exception {
        loadTest(4, 100, 20000);
    }

    @Test
    public void test04Threads_10000_2000() throws Exception {
        loadTest(4, 10000, 2000);
    }

    @Test
    public void test16Threads_10000_10000() throws Exception {
        loadTest(16, 10000, 10000);
    }

    @Test
    public void test16Threads_10000_15000() throws Exception {
        loadTest(16, 10000, 15000);
    }

    @Test
    public void test16Threads_100_20000() throws Exception {
        loadTest(16, 100, 20000);
    }

    @Test
    public void test16Threads_10000_2000() throws Exception {
        loadTest(16, 10000, 2000);
    }

    protected void loadTest(int threadsCount, int capacity, int keysCount) throws Exception {
        OccurrencesRating<String> rating = createNewRating(capacity);
        Set<String> keys = generateKeys(keysCount);
        List<String> samples = generateSamples(keys);
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(threadsCount * 100);
        System.out.printf("testing %s with %d threads; capacity: %d; keys: %d; samples: %d\n", rating.getClass().getSimpleName(), threadsCount, capacity, keysCount, samples.size());

        ExecutorService pool = Executors.newFixedThreadPool(threadsCount);
        Worker worker = new Worker(queue, rating);
        for (int i = 0; i < threadsCount; i++) {
            pool.execute(worker);
        }

        Collections.shuffle(samples, rand);
        samples.addAll(Collections.nCopies(threadsCount, TERMINATOR));
        for (String sample : samples) {
            queue.put(sample);
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        worker.printStats();

        Map<String, Integer> stats = rating.getStatistics(capacity);

        Set<String> actualKeys = new HashSet<>(stats.keySet());
        actualKeys.removeAll(keys);
        assertEquals(0, actualKeys.size());
        assertEquals(worker.count.get(), samples.size() - threadsCount);
        Integer lastWeight = null;
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            Integer weight = e.getValue();
            if (lastWeight != null) {
                assertTrue(lastWeight >= weight);
            }
            lastWeight = weight;
        }
    }

    private List<String> generateSamples(Set<String> keys) {
        List<String> samples = new ArrayList<>(keys.size() * 100);
        for (String key : keys) {
            int count = rand.nextInt(1000);
            samples.addAll(Collections.nCopies(count, key));
        }
        return samples;
    }

    private Set<String> generateKeys(int samplesCount) {
        Set<String> keys = new HashSet<>(samplesCount * 2);
        while (keys.size() < samplesCount) {
            String key = String.valueOf(rand.nextInt());
            keys.add(key);
        }
        return keys;
    }

    private static class Worker implements Runnable {
        final BlockingQueue<String> queue;
        final OccurrencesRating<String> rating;
        final AtomicLong sum = new AtomicLong();
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

        private Worker(BlockingQueue<String> queue, OccurrencesRating<String> rating) {
            this.queue = queue;
            this.rating = rating;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    String item = queue.take();
                    if (TERMINATOR.equals(item)) {
                        return;
                    }
                    long t1 = System.nanoTime();
                    rating.register(item);
                    long t2 = System.nanoTime();
                    addStat(t2 - t1);
                }
            } catch (InterruptedException ex) {
                // just finish
            }
        }

        private void addStat(long nano) {
            checkMax(nano);
            checkMin(nano);
            count.incrementAndGet();
            sum.addAndGet(nano);
        }

        private void checkMax(long nano) {
            while (true) {
                long m = max.get();
                if (m >= nano || max.compareAndSet(m, nano)) {
                    break;
                }
            }
        }

        private void checkMin(long nano) {
            while (true) {
                long m = min.get();
                if (m <= nano || min.compareAndSet(m, nano)) {
                    break;
                }
            }
        }

        void printStats() {
            System.out.printf("min time: %.2f ms\n", min.get() / 1000000.0);
            System.out.printf("max time: %.2f ms\n", max.get() / 1000000.0);
            System.out.printf("avg time: %.2f ms\n", (double) sum.get() / count.get() / 1000000.0);
            System.out.printf("calls: %d\n", count.get());
        }
    }

}
