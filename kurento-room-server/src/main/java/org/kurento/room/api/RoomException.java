package org.kurento.room.api;

public class RoomException extends RuntimeException {

	private static final long serialVersionUID = -6184177376254906794L;

	public static final int REQ_FILTER_ERROR_CODE = 107;
	public static final int NO_MEDIA_RESOURCES_ERROR_CODE = 106;
	public static final int CANNOT_CREATE_ROOM_ERROR_CODE = 105;
	public static final int EXISTING_USER_IN_ROOM_ERROR_CODE = 104;
	public static final int ROOM_CLOSED_ERROR_CODE = 103;
	public static final int USER_NOT_FOUND_ERROR_CODE = 102;
	public static final int SDP_ERROR_CODE = 101;

	private int code;

	public RoomException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	@Override
	public String toString() {
		return "Code: " + code + " " + super.toString();
	}

}
