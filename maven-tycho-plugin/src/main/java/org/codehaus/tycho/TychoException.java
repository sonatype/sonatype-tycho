package org.codehaus.tycho;

public class TychoException extends Exception {

	private static final long serialVersionUID = -3721311062142369314L;

	public TychoException() {
		super();
	}

	public TychoException(String message, Throwable cause) {
		super(message, cause);
	}

	public TychoException(String message) {
		super(message);
	}

	public TychoException(Throwable cause) {
		super(cause);
	}

}
