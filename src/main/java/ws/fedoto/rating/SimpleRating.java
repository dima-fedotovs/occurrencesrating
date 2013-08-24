/*
 * Copyright (c) 2013 Dimitrijs Fedotovs.
 *
 * This file is part of Rating project.
 *
 * Rating project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package ws.fedoto.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SimpleRating<K> implements Rating<K> {
    private final int capacity;
    private final Map<K, Node<K>> index;
    private Node<K> top;
    private Node<K> bottom;
    private Node<K> insertionPoint;

    public SimpleRating(int capacity) {
        this.capacity = capacity;
        if (capacity == Integer.MAX_VALUE) {
            this.index = new HashMap<>();
        } else {
            this.index = new HashMap<>(capacity + 1, 1.0f);
        }
    }

    @Override
    public void register(K key) {
        Node<K> node = index.get(key);
        if (node == null) {
            node = new Node<>(key);
            add(node);
            index.put(key, node);
        } else {
            promote(node);
        }
    }

    @Override
    public List<K> getTop(int count) {
        int realCount = Math.min(count, index.size());
        List<K> result = new ArrayList<>(realCount);
        Node<K> current = top;
        for (int i = 0; i < realCount; i++) {
            result.add(current.getKey());
            current = current.getNext();
        }
        return result;
    }

    private void add(Node<K> item) {
        if (top == null) {
            top = item;
            bottom = item;
            insertionPoint = item;
            item.setPrev(null);
            item.setNext(null);
            item.setWeight(1);
            return;
        }
        insert(item, insertionPoint.getPrev(), insertionPoint);
        item.setWeight(insertionPoint.getWeight());
        insertionPoint = item;
        if (index.size() == capacity) {
            index.remove(bottom.getKey());
            remove(bottom);
        }
    }

    private void promote(Node<K> item) {
        if (item == insertionPoint && item.getNext() != null) {
            insertionPoint = item.getNext();
        }
        item.incWeight();
        Node<K> newPrev = item.getPrev();
        while (newPrev != null && item.compareTo(newPrev) >= 0) {
            newPrev = newPrev.getPrev();
        }
        if (newPrev == item.getPrev()) {
            return;
        }
        remove(item);
        Node<K> newNext;
        if (newPrev == null) {
            newNext = top;
        } else {
            newNext = newPrev.getNext();
        }
        insert(item, newPrev, newNext);
    }

    private void insert(Node<K> item, Node<K> newPrev, Node<K> newNext) {
        item.setPrev(newPrev);
        item.setNext(newNext);
        if (newPrev == null) {
            top = item;
        } else {
            newPrev.setNext(item);
        }
        if (newNext == null) {
            bottom = item;
        } else {
            newNext.setPrev(item);
        }
    }

    private void remove(Node<K> item) {
        Node<K> oldPrev = item.getPrev();
        Node<K> oldNext = item.getNext();
        if (oldPrev == null) {
            top = oldNext;
        } else {
            oldPrev.setNext(oldNext);
        }
        if (oldNext == null) {
            bottom = oldPrev;
        } else {
            oldNext.setPrev(oldPrev);
        }
    }
}
