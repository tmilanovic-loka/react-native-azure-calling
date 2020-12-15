package com.reactnativeazurecalling.exceptions;

public class CallNotActiveException extends Exception {

  public static final String message = "CALL_NOT_ACTIVE_EXCEPTION";

  public CallNotActiveException() { super(message); }

}
