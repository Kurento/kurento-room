/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.room.api;

import org.kurento.client.MediaElement;

/**
 * Media-related operations that a media endpoint will implement in order to
 * allow modifications on its media stream.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface MediaShapingEndpoint {

	/**
	 * Changes the media passing through a chain of media elements by applying
	 * the specified element/shaper. The element is plugged into the stream only
	 * if the chain has been initialized (a.k.a. media streaming has started),
	 * otherwise it is left ready for when the connections between elements will
	 * materialize and the streaming begins.
	 * 
	 * @param shaper
	 *            {@link MediaElement} linked to the end of the chain (e.g. a
	 *            filter)
	 * @return the element's id
	 * @throws RoomException
	 *             if thrown, the media element was not added
	 */
	public String apply(MediaElement shaper) throws RoomException;

	/**
	 * Using the provided identifier, it removes the shaper found in the media
	 * chain structure. The corresponding media element object is released. If
	 * the chain is connected, both adjacent remaining elements will be
	 * interconnected.
	 * 
	 * @param id
	 *            the shaper (media element) identifier
	 * @throws RoomException
	 *             if thrown, the media element was not removed
	 */
	public void revert(String shaperId) throws RoomException;
}
