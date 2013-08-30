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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

/**
 *
 */
public class QueuingOccurrencesRating<K> implements OccurrencesRating<K> {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread result = new Thread(r);
                    result.setDaemon(true);
                    result.setName(QueuingOccurrencesRating.class.getSimpleName());
                    return result;
                }
            });
    private final OccurrencesRating<K> instance;
    private final Semaphore semaphore;

    public QueuingOccurrencesRating(int capacity) {
        this.instance = new SimpleOccurrencesRating<>(capacity);
        this.semaphore = new Semaphore(capacity * 3);
        size();
    }

    @Override
    public void register(K key) {
        try {
            semaphore.acquire();
            executor.submit(new RegisterWorker(key));
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<K> getTop(int count) throws IllegalStateException {
        try {
            semaphore.acquire();
            return executor.submit(new GetTopWorker(count)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<K, Integer> getStatistics(int count) {
        try {
            semaphore.acquire();
            return executor.submit(new GetStatisticWorker(count)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int size() {
        try {
            semaphore.acquire();
            return executor.submit(new GetSizeWorker()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private class RegisterWorker implements Runnable {
        final K key;

        private RegisterWorker(K key) {
            this.key = key;
        }

        @Override
        public void run() {
            try {
                instance.register(key);
            } finally {
                semaphore.release();
            }
        }
    }

    private class GetTopWorker implements Callable<List<K>> {
        private final int count;

        public GetTopWorker(int count) {
            this.count = count;
        }

        @Override
        public List<K> call() throws Exception {
            try {
                return instance.getTop(count);
            } finally {
                semaphore.release();
            }
        }
    }

    private class GetStatisticWorker implements Callable<Map<K, Integer>> {
        private final int count;

        public GetStatisticWorker(int count) {
            this.count = count;
        }

        @Override
        public Map<K, Integer> call() throws Exception {
            try {
                return instance.getStatistics(count);
            } finally {
                semaphore.release();
            }
        }
    }

    private class GetSizeWorker implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            try {
                return instance.size();
            } finally {
                semaphore.release();
            }
        }
    }
}
