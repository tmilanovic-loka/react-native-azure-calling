package com.reactnativeazurecalling.exceptions;

public class CallAgentNotInitializedException extends Exception {

  public static final String message = "CALL_AGENT_NOT_INITIALIZED_EXCEPTION";

  public CallAgentNotInitializedException() { super(message); }

}
