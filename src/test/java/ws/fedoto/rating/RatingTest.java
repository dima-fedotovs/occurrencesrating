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


import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public abstract class RatingTest {
    private static final int HAPPY_CAPACITY = 7;
    protected Rating<String> instance;

    @Test
    public void testHappyPath() throws Exception {
        instance = createNewRating(HAPPY_CAPACITY);
        check();
        //                    *1
        registerAndCheck("A", "A");
        checkWeights(e("A", 1));
        //                    *1    1
        registerAndCheck("B", "B", "A");
        checkWeights(e("B", 1), e("A", 1));
        //                     2   *1
        registerAndCheck("A", "A", "B");
        checkWeights(e("A", 2), e("B", 1));
        //                     3   *1
        registerAndCheck("A", "A", "B");
        checkWeights(e("A", 3), e("B", 1));
        //                     3   *1    1
        registerAndCheck("C", "A", "C", "B");
        checkWeights(e("A", 3), e("C", 1), e("B", 1));
        //                     3    2   *1
        registerAndCheck("C", "A", "C", "B");
        checkWeights(e("A", 3), e("C", 2), e("B", 1));
        //                     3   *2    2
        registerAndCheck("B", "A", "B", "C");
        checkWeights(e("A", 3), e("B", 2), e("C", 2));
        //                     3    3   *2
        registerAndCheck("C", "C", "A", "B");
        checkWeights(e("C", 3), e("A", 3), e("B", 2));
        //                     3    3   *2    2
        registerAndCheck("D", "C", "A", "D", "B");
        checkWeights(e("C", 3), e("A", 3), e("D", 2), e("B", 2));
        //                     3    3    3   *2
        registerAndCheck("D", "D", "C", "A", "B");
        //                    *3    3    3    3
        registerAndCheck("B", "B", "D", "C", "A");
        //                    *3    3    3    3    3
        registerAndCheck("E", "E", "B", "D", "C", "A");
        //                     4   *3    3    3    3
        registerAndCheck("D", "D", "E", "B", "C", "A");
        //                     4    4   *3    3    3
        registerAndCheck("A", "A", "D", "E", "B", "C");
        //                     4    4   *3    3    3    3
        registerAndCheck("F", "A", "D", "F", "E", "B", "C");
        checkWeights(e("A", 4), e("D", 4), e("F", 3), e("E", 3), e("B", 3), e("C", 3));
        //                     4    4   *3    3    3    3    3
        registerAndCheck("G", "A", "D", "G", "F", "E", "B", "C");
        //                     4    4    4   *3    3    3    3
        registerAndCheck("G", "G", "A", "D", "F", "E", "B", "C");
        //                     4    4    4   *3    3    3    3
        registerAndCheck("H", "G", "A", "D", "H", "F", "E", "B");
        //                     4    4    4   *3    3    3    3
        registerAndCheck("C", "G", "A", "D", "C", "H", "F", "E");
        //                     4    4    4    4   *3    3    3
        registerAndCheck("H", "H", "G", "A", "D", "C", "F", "E");
        //                     5    4    4    4   *3    3    3
        registerAndCheck("G", "G", "H", "A", "D", "C", "F", "E");
        //                     5    4    4    4    4   *3    3
        registerAndCheck("F", "G", "F", "H", "A", "D", "C", "E");
        //                     5    4    4    4    4    4   *3
        registerAndCheck("E", "G", "E", "F", "H", "A", "D", "C");
        //                     5    5    4    4    4    4   *3
        registerAndCheck("D", "D", "G", "E", "F", "H", "A", "C");
        //                     5    5   *4    4    4    4    4
        registerAndCheck("C", "D", "G", "C", "E", "F", "H", "A");
        //                     5    5    5   *4    4    4    4
        registerAndCheck("C", "C", "D", "G", "E", "F", "H", "A");
        //                     5    5    5   *4    4    4    4
        registerAndCheck("B", "C", "D", "G", "B", "E", "F", "H");
        //                     5    5    5   *4    4    4    4
        registerAndCheck("Z", "C", "D", "G", "Z", "B", "E", "F");
        checkWeights(e("C", 5), e("D", 5), e("G", 5), e("Z", 4), e("B", 4), e("E", 4), e("F", 4));
    }

    protected void checkWeights(Entry... expectedEntries) throws Exception {
        Map<String, Integer> sample = instance.getStatistics(HAPPY_CAPACITY);
        assertEquals(expectedEntries.length, sample.size());

        Iterator<Entry> iterator = Arrays.asList(expectedEntries).iterator();
        for (Map.Entry<String, Integer> e : sample.entrySet()) {
            Entry expectedEntry = iterator.next();
            assertEquals(expectedEntry.key, e.getKey());
            assertEquals(expectedEntry.weight, e.getValue());
        }
    }

    private Entry e(String key, int weight) {
        return new Entry(key, weight);
    }

    protected void registerAndCheck(String newKey, String... expectedItems) {
        instance.register(newKey);
        check(expectedItems);
    }

    protected void check(String... expectedItems) {
        List<String> actual = instance.getTop(HAPPY_CAPACITY);
        List<String> expected = Arrays.asList(expectedItems);
        assertEquals(expected, actual);
    }

    protected abstract Rating<String> createNewRating(int capacity);

    private static class Entry {
        String key;
        Integer weight;

        private Entry(String key, Integer weight) {
            this.key = key;
            this.weight = weight;
        }
    }
}
