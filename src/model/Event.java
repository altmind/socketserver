package model;

import java.util.HashMap;
import java.util.Map;

public class Event {
	private String logger;
	private String level;
	private String thread;
	private Long timestamp;
	private String message;
	private String throwable;
	private Map<String,String> properties = new HashMap<String,String>();
	/**
	 * @return the logger
	 */
	public String getLogger() {
		return logger;
	}
	/**
	 * @param logger the logger to set
	 */
	public void setLogger(String logger) {
		this.logger = logger;
	}
	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}
	/**
	 * @return the thread
	 */
	public String getThread() {
		return thread;
	}
	/**
	 * @param thread the thread to set
	 */
	public void setThread(String thread) {
		this.thread = thread;
	}
	/**
	 * @return the timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the throwable
	 */
	public String getThrowable() {
		return throwable;
	}
	/**
	 * @param throwable the throwable to set
	 */
	public void setThrowable(String throwable) {
		this.throwable = throwable;
	}
	/**
	 * @return the properties
	 */
	public Map<String,String> getProperties() {
		return properties;
	}

}
