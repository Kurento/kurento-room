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

package org.kurento.room.exception;

public class RoomException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Code {
		GENERIC_ERROR_CODE(999),
		MUTE_MEDIA_ERROR_CODE(112),
		USER_NOT_STREAMING_ERROR_CODE(111),
		NOT_WEB_ENDPOINT_ERROR_CODE(110),
		RTP_ENDPOINT_ERROR_CODE(108),
		WEBRTC_ENDPOINT_ERROR_CODE(108),
		MEDIA_ENDPOINT_ERROR_CODE(107),
		ROOM_NOT_FOUND_ERROR_CODE(106),
		CANNOT_CREATE_ROOM_ERROR_CODE(105),
		EXISTING_USER_IN_ROOM_ERROR_CODE(104),
		ROOM_CLOSED_ERROR_CODE(103),
		USER_NOT_FOUND_ERROR_CODE(102),
		SDP_ERROR_CODE(101);

		private int value;

		private Code(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	private Code code = Code.GENERIC_ERROR_CODE;

	public RoomException(Code code, String message) {
		super(message);
		this.code = code;
	}

	public Code getCode() {
		return code;
	}

	public int getCodeValue() {
		return code.getValue();
	}

	@Override
	public String toString() {
		return "Code: " + getCodeValue() + " " + super.toString();
	}

}
