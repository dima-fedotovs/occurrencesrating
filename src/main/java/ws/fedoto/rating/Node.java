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

/**
 *
 */
class Node<K> implements Comparable<Node<K>> {
    private K key;
    private int weight;
    private Node<K> next;
    private Node<K> prev;

    public Node(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public int getWeight() {
        return weight;
    }

    void setWeight(int weight) {
        this.weight = weight;
    }

    public void incWeight() {
        this.weight++;
    }

    public Node<K> getNext() {
        return next;
    }

    public void setNext(Node<K> next) {
        this.next = next;
    }

    public Node<K> getPrev() {
        return prev;
    }

    public void setPrev(Node<K> prev) {
        this.prev = prev;
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
