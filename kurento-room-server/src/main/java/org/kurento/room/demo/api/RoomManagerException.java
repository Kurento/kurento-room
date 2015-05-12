package org.kurento.room.demo.api;

public class RoomManagerException extends RuntimeException {

	private static final long serialVersionUID = -6184177376254906794L;

	private int code;

	public RoomManagerException(int code, String message) {
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
