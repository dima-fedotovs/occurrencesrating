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

import java.util.*;

/**
 *
 */
public class SimpleOccurrencesRating<K> implements OccurrencesRating<K> {
    private final int capacity;
    private final Map<K, Node<K>> index;
    private Node<K> top;
    private Node<K> bottom;
    private Node<K> insertionPoint;

    public SimpleOccurrencesRating(int capacity) {
        this.capacity = capacity;
        if (capacity == Integer.MAX_VALUE) {
            this.index = new HashMap<>();
        } else {
            this.index = new HashMap<>(capacity * 2);
        }
    }

    @Override
    public void register(K key) {
        Node<K> node = index.get(key);
        if (node == null) {
            node = new Node<>(key);
            add(node);
        } else {
            promote(node);
        }
    }

    @Override
    public List<K> getTop(int count) {
        int realCount = Math.min(count, index.size());
        List<K> result = new ArrayList<>(realCount);
        for (Node<K> current = top; current != null; current = current.next) {
            result.add(current.key);
        }
        return result;
    }

    @Override
    public Map<K, Integer> getStatistics(int count) {
        int realCount = Math.min(count, index.size());
        Map<K, Integer> result = new LinkedHashMap<>(realCount * 2);
        for (Node<K> current = top; current != null; current = current.next) {
            result.put(current.key, current.weight);
        }
        return result;
    }

    @Override
    public int size() {
        return index.size();
    }

    private void add(Node<K> item) {
        index.put(item.key, item);
        if (top == null) {
            top = item;
            bottom = item;
            insertionPoint = item;
            item.prev = null;
            item.next = null;
            item.weight = 1;
            return;
        }
        insert(item, insertionPoint.prev, insertionPoint);
        item.weight = insertionPoint.weight;
        insertionPoint = item;
        if (index.size() > capacity) {
            index.remove(bottom.key);
            remove(bottom);
        }
    }

    private void promote(Node<K> item) {
        if (item == insertionPoint && item.next != null) {
            insertionPoint = item.next;
        }
        item.weight++;
        Node<K> newPrev = item.prev;
        while (newPrev != null && item.compareTo(newPrev) >= 0) {
            newPrev = newPrev.prev;
        }
        if (newPrev == item.prev) {
            return;
        }
        remove(item);
        Node<K> newNext;
        if (newPrev == null) {
            newNext = top;
        } else {
            newNext = newPrev.next;
        }
        insert(item, newPrev, newNext);
    }

    private void insert(Node<K> item, Node<K> newPrev, Node<K> newNext) {
        item.prev = newPrev;
        item.next = newNext;
        if (newPrev == null) {
            top = item;
        } else {
            newPrev.next = item;
        }
        if (newNext == null) {
            bottom = item;
        } else {
            newNext.prev = item;
        }
    }

    private void remove(Node<K> item) {
        Node<K> oldPrev = item.prev;
        Node<K> oldNext = item.next;
        if (oldPrev == null) {
            top = oldNext;
        } else {
            oldPrev.next = oldNext;
        }
        if (oldNext == null) {
            bottom = oldPrev;
        } else {
            oldNext.prev = oldPrev;
        }
    }

    static private class Node<K> implements Comparable<Node<K>> {
        private K key;
        private int weight;
        private Node<K> next;
        private Node<K> prev;

        public Node(K key) {
            this.key = key;
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
