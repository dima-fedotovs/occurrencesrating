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

import java.util.List;

/**
 * @author dima
 */
public interface Rating<K> {
    public void register(K key);

    public List<K> getTop(int count);
}
