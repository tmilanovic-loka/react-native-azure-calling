package com.reactnativeazurecalling;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.azure.android.communication.calling.IncomingCallListener;
import com.azure.android.communication.calling.RemoteParticipant;
import com.azure.android.communication.calling.IncomingCall;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.PhoneNumberIdentifier;
import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallEndReason;
import com.azure.android.communication.calling.CallState;
import com.azure.android.communication.calling.CallDirection;
import com.azure.android.communication.calling.HangUpOptions;
import com.azure.android.communication.calling.PropertyChangedEvent;
import com.azure.android.communication.calling.PropertyChangedListener;
import com.azure.android.communication.calling.StartCallOptions;
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

  CallClient callClient = null;
  CallAgent callAgent = null;
  Call call = null;

  PropertyChangedListener callStateChangeListener = new PropertyChangedListener() {
    @Override
    public void onPropertyChanged(PropertyChangedEvent args) {
      String callState = call.getState().toString();
      WritableMap params = Arguments.createMap();
      params.putString("callState", callState);
      sendEvent(getReactApplicationContext(), "CALL_STATE_CHANGED", params);
    }
  };

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

  @ReactMethod
  public void ping(String from, Promise promise) {
    String result = "`ping` received from " + from;
    Log.d("JavaLog", result);
    promise.resolve(result);
  }

  private class MyIncomingCallListener implements IncomingCallListener {
    public MyIncomingCallListener() {
      super();
      Log.d("JavaLog", "I HATH BEEN MADE");
    }
    public void onIncomingCall(IncomingCall incomingCall) {
      Log.d("JavaLog", "INCOMING CALL DETECTED!");
      Context context = getReactApplicationContext().getApplicationContext();
      incomingCall.accept(context);
    }
  }

  @ReactMethod
  public void createAgent(String userToken, Promise promise) {
    Context context = getReactApplicationContext().getApplicationContext();
    CommunicationTokenCredential credential = new CommunicationTokenCredential(userToken);
    try {
      if (callClient == null) {
        callClient = new CallClient();
      }
      if (callAgent == null) {
        callAgent = callClient.createCallAgent(context, credential).get();
      }
      MyIncomingCallListener myIncomingCallListener = new MyIncomingCallListener();
      callAgent.addOnIncomingCallListener(myIncomingCallListener);
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
    List<CommunicationIdentifier> participants = new ArrayList<CommunicationIdentifier>(userStrings.size());
    for (int i = 0; i < userStrings.size(); i++) {
      participants.add(new CommunicationUserIdentifier(userStrings.get(i).toString()));
    }
    call = callAgent.startCall(getContext(), participants);
    call.addOnStateChangedListener(callStateChangeListener);
    promise.resolve(call.getId());
  }

  @ReactMethod
  public void startRecording(Promise promise) {
    
  }

  @ReactMethod
  public void stopRecording(Promise promise) {
    
  }

  @ReactMethod
  public void addParticipant(String userID, Promise promise) {
    if (call == null) {
      Log.d("JavaLog", "ADD PARTICIPANT: CALL INACTIVE");
      promise.reject(new CallNotActiveException());
      return;
    }
    Log.d("JavaLog", "ADDING: " + userID);
    CommunicationIdentifier participant = new CommunicationUserIdentifier(userID);
    if (participant == null) {
      Log.d("JavaLog", "FAILED TO ADD PARTICIPANT");
    }
    
    RemoteParticipant addedParticipant = call.addParticipant(participant);
    if (addedParticipant == null) {
      Log.d("JavaLog", "ADDED PARTICIPANT IS NULL");
    }
    Log.d("JavaLog", "Participant name: " + addedParticipant.getDisplayName());
    Log.d("JavaLog", "Participant state: " + addedParticipant.getState());
  }

  @ReactMethod
  public void callPSTN(String from, String to, Promise promise) {
    if (callAgent == null) {
      promise.reject(new CallAgentNotInitializedException());
      return;
    }
    StartCallOptions options = new StartCallOptions();
    options.setAlternateCallerId(new PhoneNumberIdentifier(from));

    List<CommunicationIdentifier> participants = new ArrayList<CommunicationIdentifier>();
    participants.add(new PhoneNumberIdentifier(to));

    call = callAgent.startCall(getContext(), participants, options);
    call.addOnStateChangedListener(callStateChangeListener);
    promise.resolve(call.getId());
  }

  @ReactMethod
  public void muteCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    call.mute(getContext());
    promise.resolve(null);
  }

  @ReactMethod
  public void unMuteCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    call.unmute(getContext());
    promise.resolve(null);
  }

  @ReactMethod
  public void hangUpCall(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    CallState callState = call.getState();
    List<String> VALID_STATES = Arrays.asList("Connecting", "Ringing", "EarlyMedia", "Connected", "LocalHold", "RemoteHold");
    if (VALID_STATES.contains(callState.toString())) {
      HangUpOptions options = new HangUpOptions();
      call.hangUp(options);
      promise.resolve(null);
    }
  }

  @ReactMethod
  public void getCurrentCallID(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getId());
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
    promise.resolve(call.isMuted());
  }

  @ReactMethod
  public void getCurrentCallIncoming(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getDirection() == CallDirection.INCOMING);
  }

  @ReactMethod
  public void getCurrentCallCallerID(Promise promise) {
    if (call == null) {
      promise.reject(new CallNotActiveException());
      return;
    }
    promise.resolve(call.getCallerInfo().getIdentifier().toString());
  }
}
