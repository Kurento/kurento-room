/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.room.demo;

import com.google.gson.JsonObject;

public class RpcNotification {
	private String method;
	private JsonObject params;

	public RpcNotification(String method, JsonObject params) {
		this.method = method;
		this.params = params;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JsonObject getParams() {
		return params;
	}

	public void setParams(JsonObject params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return "[method=" + method + ", params=" + params + "]";
	}
}
