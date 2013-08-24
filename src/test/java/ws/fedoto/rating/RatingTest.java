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
import java.util.List;

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
        //                    *1    1
        registerAndCheck("B", "B", "A");
        //                     2   *1
        registerAndCheck("A", "A", "B");
        //                     3   *1
        registerAndCheck("A", "A", "B");
        //                     3   *1    1
        registerAndCheck("C", "A", "C", "B");
        //                     3    2   *1
        registerAndCheck("C", "A", "C", "B");
        //                     3   *2    2
        registerAndCheck("B", "A", "B", "C");
        //                     3    3   *2
        registerAndCheck("C", "C", "A", "B");
        //                     3    3   *2    2
        registerAndCheck("D", "C", "A", "D", "B");
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
}
