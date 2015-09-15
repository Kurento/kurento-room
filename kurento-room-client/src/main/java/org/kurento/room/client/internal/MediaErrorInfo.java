/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.room.client.internal;

import org.kurento.room.internal.ProtocolElements;

/**
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 * 
 * @see Notification
 */
public class MediaErrorInfo extends Notification {

	private String description;
	
	public MediaErrorInfo(String description) {
		super(ProtocolElements.MEDIAERROR_METHOD);
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (getMethod() != null)
			builder.append("method=").append(getMethod()).append(", ");
		if (description != null)
			builder.append("description=").append(description);
		builder.append("]");
		return builder.toString();
	}
}
