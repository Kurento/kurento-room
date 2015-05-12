package org.kurento.room.demo.internal;

public class ThreadLogUtils {

	public static final String HANDLER_THREAD_NAME = "handler";

	public static void updateThreadName(String name) {
		Thread.currentThread().setName(name);
	}
}
