package com.reactnativeazurecalling;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationUser;
import com.azure.android.communication.common.CommunicationUserCredential;
import com.azure.android.communication.common.PhoneNumber;
import com.azure.communication.calling.Call;
import com.azure.communication.calling.CallAgent;
import com.azure.communication.calling.CallClient;
import com.azure.communication.calling.CallEndReason;
import com.azure.communication.calling.CallState;
import com.azure.communication.calling.HangupOptions;
import com.azure.communication.calling.PropertyChangedEvent;
import com.azure.communication.calling.PropertyChangedListener;
import com.azure.communication.calling.StartCallOptions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativeazurecalling.exceptions.CallAgentNotInitializedException;
import com.reactnativeazurecalling.exceptions.CallNotActiveException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AzureCallingModule extends ReactContextBaseJavaModule {

  CallAgent callAgent = null;
  Call call = null;

  AzureCallingModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  public String getName() {
    return "AzureCalling";
  }

  private Context getContext() {
    return getReactApplicationContext().getApplicationContext();
  }

  private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }

  PropertyChangedListener callStateChangeListener = new PropertyChangedListener() {
    @Override
    public void onPropertyChanged(PropertyChangedEvent args) {
      String callState = call.getState().toString();
      Log.d("Event", "The call state has changed to: " + callState);
      WritableMap params = Arguments.createMap();
      params.putString("callState", callState);
      sendEvent(getReactApplicationContext(), "CALL_STATE_CHANGED", params);
    }
  };

  @ReactMethod
  public void ping(String from, Promise promise) {
    String result = "`ping` received from " + from;
    Log.d("JavaLog", result);
    promise.resolve(result);
  }

  @ReactMethod
  public void createAgent(String userToken, Promise promise) {
    Context context = getReactApplicationContext().getApplicationContext();
    CommunicationUserCredential credential = new CommunicationUserCredential(userToken);
    try {
      callAgent = new CallClient().createCallAgent(context, credential).get();
      promise.resolve(null);
    } catch (ExecutionException e) {
      promise.reject(e);
    } catch (InterruptedException e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void callACSUsers(ReadableArray users, Promise promise) {
    if (callAgent == null) {
      promise.reject(new CallAgentNotInitializedException());
      return;
    }
    ArrayList<Object> userStrings = users.toArrayList();
    CommunicationIdentifier participants[] = new CommunicationIdentifier[userStrings.size()];
    for (int i = 0; i < userStrings.size(); i++) {
      participants[i] = new CommunicationUser(userStrings.get(i).toString());
      ;
    }
    StartCallOptions options = new StartCallOptions();
    call = callAgent.call(getContext(), participants, options);
    promise.resolve(call.getCallId());
  }

  @ReactMethod
  public void callPSTN(String from, String to, Promise promise) {
    if (callAgent == null) {
      promise.reject(new CallAgentNotInitializedException());
      return;
    }
    StartCallOptions options = new StartCallOptions();
    options.setAlternateCallerId(new PhoneNumber(from));
    call = callAgent.call(getContext(), new PhoneNumber[]{new PhoneNumber(to)}, options);
    promise.resolve(call.getCallId());
  }

  @ReactMethod
  public void muteCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    call.mute();
    promise.resolve(null);
  }

  @ReactMethod
  public void unMuteCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    call.unmute();
    promise.resolve(null);
  }

  @ReactMethod
  public void hangUpCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    CallState callState = call.getState();
    List<String> VALID_STATES = Arrays.asList("Connecting", "Ringing", "EarlyMedia", "Connected", "Hold");
    if (VALID_STATES.contains(callState.toString())) {
      HangupOptions options = new HangupOptions();
      call.hangup(options);
      call.removeOnCallStateChangedListener(callStateChangeListener);
      Log.d("TRYING TO HANGUP", "hangupCall()");
      promise.resolve(null);
    }
  }

  @ReactMethod
  public void getCurrentCallID(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getCallId());
  }

  @ReactMethod
  public void getCurrentCallState(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getState().toString());
  }

  @ReactMethod
  public void getCurrentCallEndReason(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    CallEndReason callEndReason = call.getCallEndReason();
    WritableMap output = new WritableNativeMap();
    output.putInt("code", callEndReason.getCode());
    output.putInt("sub_code", callEndReason.getSubcode());
    promise.resolve(output);
  }

  @ReactMethod
  public void getCurrentCallMicrophoneMuted(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getIsMicrophoneMuted());
  }

  @ReactMethod
  public void getCurrentCallIncoming(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getIsIncoming());
  }

  @ReactMethod
  public void getCurrentCallCallerID(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getCallerId().toString());
  }

  @ReactMethod
  public void callPSTN(String from, String to) {
    Context context = getReactApplicationContext().getApplicationContext();
    PhoneNumber callerPhone = new PhoneNumber(from);
    StartCallOptions options = new StartCallOptions();
    options.setAlternateCallerId(callerPhone);
    PhoneNumber number = new PhoneNumber(to);
    call = callAgent.call(context, new PhoneNumber[]{number}, options);
    call.addOnCallStateChangedListener(callStateChangeListener);
  }
}
