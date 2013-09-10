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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 */
public class ConcurrentOccurrencesRating<K> implements OccurrencesRating<K> {
    private final int capacity;
    private final ConcurrentHashMap<K, Node<K>> index;
    private final Node<K> topHolder = new Node<>(null);
    private final Node<K> bottomHolder = new Node<>(null);
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicReference<Node<K>> insertionPointHolder = new AtomicReference<>(bottomHolder);
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    public ConcurrentOccurrencesRating(int capacity) {
        this.capacity = capacity;
        this.index = new ConcurrentHashMap<>(capacity * 2);
        this.topHolder.next = bottomHolder;
        this.bottomHolder.prev = topHolder;
        this.bottomHolder.weight = 1;
    }

    @Override
    public void register(K key) {
        globalLock.readLock().lock();
        try {
            Node<K> node = index.get(key);
            if (node == null) {
                node = new Node<>(key);
                node.lock();
                try {
                    Node<K> existsNode = index.putIfAbsent(key, node);
                    if (existsNode != null) {
                        promote(existsNode);
                    } else {
                        add(node);
                    }
                } finally {
                    node.unlock();
                }
            } else {
                promote(node);
            }
        } finally {
            globalLock.readLock().unlock();
        }
    }

    private boolean promote(Node<K> item) {
        Node<K>[] locked = new Node[4];
        while (true) {
            Node<K> next;
            item.lock();
            try {
                if (item.weight < 0) {
                    return false;
                }
                next = item.next;
            } finally {
                item.unlock();
            }
            try {
                next.lock();
                locked[3] = next;
                if (next.prev != item) {
                    continue;
                }
                item.lock();
                locked[2] = item;
                item.weight++;
                locked[2].prev.lock();
                locked[1] = locked[2].prev;
                while (locked[1] != topHolder && locked[2].compareTo(locked[1]) >= 0) {
                    locked[1].prev.lock();
                    locked[0] = locked[1].prev;
                    if (locked[3] != bottomHolder && insertionPointHolder.get() == locked[2] && locked[2].compareTo(locked[3]) > 0) {
                        insertionPointHolder.set(locked[3]);
                    }
                    locked[2].next = locked[3].prev = locked[1];
                    locked[1].prev = locked[2];
                    locked[1].next = locked[3];
                    locked[2].prev = locked[0];
                    locked[0].next = locked[2];
                    locked[2] = locked[1];
                    locked[1] = locked[0].next;
                    locked[3].unlock();
                    System.arraycopy(locked, 0, locked, 1, 3);
                    locked[0] = null;
                }
                return true;
            } finally {
                for (int i = 0; i < 4; i++) {
                    if (locked[i] != null) {
                        locked[i].unlock();
                        locked[i] = null;
                    }
                }
            }
        }
    }

    private void add(Node<K> item) {
        while (true) {
            Node<K> ip = insertionPointHolder.get();
            ip.lock();
            try {
                if (insertionPointHolder.get() != ip) {
                    continue;
                }
                Node<K> ipp = ip.prev;
                ipp.lock();
                try {
                    insertionPointHolder.set(item);
                    ip.prev.next = item;
                    ip.prev = item;
                    item.prev = ipp;
                    item.next = ip;
                    item.weight = ip.weight;
                    size.incrementAndGet();
                } finally {
                    ipp.unlock();
                }
            } finally {
                ip.unlock();
            }
            break;
        }
        if (size.get() > capacity) {
            bottomHolder.lock();
            Node<K> last = bottomHolder.prev;
            last.lock();
            try {
                if (size.get() > capacity) {
                    last.prev.lock();
                    Node<K> preLast = last.prev;
                    last.prev = null;
                    last.next = null;
                    last.weight = -1;
                    bottomHolder.prev = preLast;
                    preLast.next = bottomHolder;
                    index.remove(last.key);
                    last.unlock();
                    last = preLast;
                    size.decrementAndGet();
                }
            } finally {
                last.unlock();
                bottomHolder.unlock();
            }
        }
    }

    @Override
    public List<K> getTop(int count) {
        globalLock.writeLock().lock();
        try {
            int realCount = Math.min(count, size.get());
            List<K> result = new ArrayList<>(realCount);
            for (Node<K> current = topHolder.next; current != bottomHolder; current = current.next) {
                result.add(current.key);
            }
            return result;
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public Map<K, Integer> getStatistics(int count) {
        globalLock.writeLock().lock();
        try {
            int realCount = Math.min(count, size.get());
            Map<K, Integer> result = new LinkedHashMap<>(realCount * 2);
            for (Node<K> current = topHolder.next; current != bottomHolder; current = current.next) {
                result.put(current.key, current.weight);
            }
            return result;
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        globalLock.writeLock().lock();
        try {
            return size.get();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    static private class Node<K> implements Comparable<Node<K>> {
        private K key;
        private int weight;
        private Node<K> next;
        private Node<K> prev;
        private Lock lock = new ReentrantLock();

        public Node(K key) {
            this.key = key;
        }

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }

        @Override
        public int compareTo(Node<K> o) {
            return weight - o.weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            return key.equals(node.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", weight=" + weight +
                    '}';
        }
    }
}
