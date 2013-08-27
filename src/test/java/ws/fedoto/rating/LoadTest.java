/*
 * Copyright (c) 2013 Dimitrijs Fedotovs.
 *
 * This file is part of Rating library.
 *
 * Rating library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rating library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rating library.  If not, see <http://www.gnu.org/licenses/>.
 */

package ws.fedoto.rating;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 *
 */
public abstract class LoadTest {
    private static final Random rand = new Random();
    private static final String TERMINATOR = "TERMINATE";

    protected abstract Rating<String> createNewRating(int capacity);


    protected void stressTest(int threadsCount, int capacity, int samplesCount) throws Exception {
        Rating<String> rating = createNewRating(capacity);
        List<String> samples = generateSamples(samplesCount);
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(threadsCount * 100);

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
        assertEquals(Math.min(capacity, samplesCount), stats.size());
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

    private List<String> generateSamples(int samplesCount) {
        List<String> samples = new ArrayList<>(samplesCount);
        for (int i = 0; i < samplesCount; i++) {
            String key = String.valueOf(rand.nextInt());
            int count = rand.nextInt(1000);
            samples.addAll(Collections.nCopies(count, key));
        }
        return samples;
    }

    private static class Worker implements Runnable {
        final BlockingQueue<String> queue;
        final Rating<String> rating;
        final AtomicLong sum = new AtomicLong();
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

        private Worker(BlockingQueue<String> queue, Rating<String> rating) {
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
