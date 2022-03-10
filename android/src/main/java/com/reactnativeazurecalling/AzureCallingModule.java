package com.reactnativeazurecalling;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.view.View;

import androidx.annotation.Nullable;

import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.PhoneNumberIdentifier;
import com.azure.android.communication.calling.*;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativeazurecalling.exceptions.CallAgentNotInitializedException;
import com.reactnativeazurecalling.exceptions.CallNotActiveException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java9.util.concurrent.CompletableFuture;
import java.util.UUID;

public class AzureCallingModule extends ReactContextBaseJavaModule {
  private enum CameraType {
    FRONT,
    BACK
  }

  private CallClient callClient = null;
  private CallAgent callAgent = null;
  private Call call = null;
  private IncomingCall incomingCall;
  private LocalVideoStream localVideoStream = null;
  private VideoStreamRenderer previewRenderer;
  private VideoStreamRenderer remoteRenderer;
  private CompletableFuture<DeviceManager> deviceManagerCompletableFuture;
  private Map<CameraType, VideoDeviceInfo> availableCameras;
  
  PropertyChangedListener callStateChangeListener = new PropertyChangedListener() {
    @Override
    public void onPropertyChanged(PropertyChangedEvent args) {
      CallState callState = call.getState();
      String callStateStr = callState.toString();
      WritableMap params = Arguments.createMap();
      params.putString("callState", callStateStr);
      sendEvent(getReactApplicationContext(), "CALL_STATE_CHANGED", params);
    }
  };

  ParticipantsUpdatedListener participantsUpdatedListener = new ParticipantsUpdatedListener() {
    @Override
    public void onParticipantsUpdated(ParticipantsUpdatedEvent event) {
      List<RemoteParticipant> remoteParticipants = event.getAddedParticipants();

      for (RemoteParticipant remoteParticipant : remoteParticipants) {
        List<RemoteVideoStream> remoteStreams = remoteParticipant.getVideoStreams();
        for (RemoteVideoStream remoteStream : remoteStreams) {
          if (remoteStream.isAvailable()) {
            addRemoteVideoStream(remoteStream);
            break;
          }
        }
        remoteParticipant.addOnVideoStreamsUpdatedListener(remoteVideoStreamsUpdatedListener);
      }
    }
  };

  RemoteVideoStreamsUpdatedListener remoteVideoStreamsUpdatedListener = new RemoteVideoStreamsUpdatedListener() {
    @Override
    public void onRemoteVideoStreamsUpdated(RemoteVideoStreamsEvent event) {
      List<RemoteVideoStream> addedRemoteStreams = event.getAddedRemoteVideoStreams();
      for (RemoteVideoStream remoteStream : addedRemoteStreams) {
        if (remoteStream.isAvailable()) {
          addRemoteVideoStream(remoteStream);
          break;
        }
      }

      List<RemoteVideoStream> removedRemoteStreams = event.getRemovedRemoteVideoStreams();
      for (RemoteVideoStream remoteStream : removedRemoteStreams) {
        if (remoteStream.isAvailable()) {
          removeRemoteVideoStream(remoteStream);
          break;
        }
      }
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

  private void createDeviceManager() {
    callClient.getDeviceManager(getContext()).thenAccept(deviceManager -> {
      deviceManagerCompletableFuture.complete(deviceManager);
    });
  }

  private void initializeCameras() {
    deviceManagerCompletableFuture.whenComplete((deviceManager, throwable) -> {
      availableCameras = new HashMap<>();

      final List<VideoDeviceInfo> initialCameras = deviceManager.getCameras();
      addVideoDevices(initialCameras);
    });
  }

  private void addVideoDevices(final List<VideoDeviceInfo> addedVideoDevices) {
    for (final VideoDeviceInfo addedVideoDevice: addedVideoDevices) {
      if (addedVideoDevice.getCameraFacing().name().equalsIgnoreCase(CameraType.FRONT.name())) {
        availableCameras.put(CameraType.FRONT, addedVideoDevice);
      } else if (addedVideoDevice.getCameraFacing().name().equalsIgnoreCase(CameraType.BACK.name())) {
        availableCameras.put(CameraType.BACK, addedVideoDevice);
      }
    }
  }

  private VideoDeviceInfo getFrontCamera() {
    return availableCameras.get(CameraType.FRONT);
  }

  private VideoDeviceInfo getBackCamera() {
    return availableCameras.get(CameraType.BACK);
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
  
  private void addLocalVideoStream() {
    UiThreadUtil.runOnUiThread(() -> {
      if (localVideoStream != null) {
        if (previewRenderer != null) {
          previewRenderer.dispose();
          previewRenderer = null;
        }
        previewRenderer = new VideoStreamRenderer(localVideoStream, getContext());
        VideoStreamView parentView = (VideoStreamView) LocalVideoViewManager.GetView();
        parentView.removeAllViews();
        parentView.addView(previewRenderer.createView(new CreateViewOptions(ScalingMode.CROP)));
        Log.i("Native/VideoEvent", "Added localVideoStream");
      }
    });
  }

  private void addRemoteVideoStream(RemoteVideoStream remoteStream) {    
    UiThreadUtil.runOnUiThread(() -> {
      if (remoteRenderer != null) {
        remoteRenderer.dispose();
        remoteRenderer = null;
      }
      remoteRenderer = new VideoStreamRenderer(remoteStream, getContext());
      VideoStreamView parentView = (VideoStreamView) RemoteVideoViewManager.GetView();
      parentView.removeAllViews();
      parentView.addView(remoteRenderer.createView(new CreateViewOptions(ScalingMode.CROP)));
      Log.i("Native/VideoEvent", "Added remoteVideoStream");
    });
  }

  private void removeRemoteVideoStream(RemoteVideoStream remoteStream) {    
    UiThreadUtil.runOnUiThread(() -> {
      if (remoteRenderer != null) {
        remoteRenderer.dispose();
        remoteRenderer = null;
      }
      VideoStreamView parentView = (VideoStreamView) RemoteVideoViewManager.GetView();
      parentView.removeAllViews();
      Log.i("Native/VideoEvent", "Removed remoteVideoStream");
    });
  }

  private void handleIncomingCall() {
    if (callAgent != null) {
      callAgent.addOnIncomingCallListener((incomingCall) -> {
          this.incomingCall = incomingCall;
          Executors.newCachedThreadPool().submit(this::answerIncomingCall);
          Log.i("Native/VideoEvent", "Incoming call");
      });

      Log.i("Native/VideoEvent", "Added incomingCallListener");
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
        handleIncomingCall();
      }

      deviceManagerCompletableFuture = new CompletableFuture<>();
      createDeviceManager();

      promise.resolve(null);
    } catch (ExecutionException e) {
      promise.reject(e);
    } catch (InterruptedException e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void callACSUsers(ReadableArray users, Promise promise) {
    deviceManagerCompletableFuture.whenComplete((deviceManager, throwable) -> {
      if (callAgent == null) {
        promise.reject(new CallAgentNotInitializedException());
        return;
      }
      initializeCameras();

      ArrayList<Object> userStrings = users.toArrayList();
      List<CommunicationIdentifier> participants = new ArrayList<CommunicationIdentifier>(userStrings.size());
      for (int i = 0; i < userStrings.size(); i++) {
        participants.add(new CommunicationUserIdentifier(userStrings.get(i).toString()));
      }
    
      StartCallOptions callOptions = new StartCallOptions();

      final VideoDeviceInfo desiredCamera = getBackCamera();
      localVideoStream = new LocalVideoStream(desiredCamera, getContext());
      final LocalVideoStream[] localVideoStreams = new LocalVideoStream[1];
      localVideoStreams[0] = localVideoStream;
      callOptions.setVideoOptions(new VideoOptions(localVideoStreams));

      call = callAgent.startCall(getContext(), participants, callOptions);
      call.addOnStateChangedListener(callStateChangeListener);
      call.addOnRemoteParticipantsUpdatedListener(participantsUpdatedListener);

      List<RemoteParticipant> remoteParticipants = call.getRemoteParticipants();
      for (RemoteParticipant remoteParticipant : remoteParticipants) {        
        remoteParticipant.addOnVideoStreamsUpdatedListener(remoteVideoStreamsUpdatedListener);
      }

      addLocalVideoStream();

      promise.resolve(call.getId());
    });
  }

  private void answerIncomingCall() {
    if (incomingCall == null) {
        return;
    }
    AcceptCallOptions acceptCallOptions = new AcceptCallOptions();

    initializeCameras();

    final VideoDeviceInfo desiredCamera = getBackCamera();
    localVideoStream = new LocalVideoStream(desiredCamera, getContext());
    final LocalVideoStream[] localVideoStreams = new LocalVideoStream[1];
    localVideoStreams[0] = localVideoStream;
    VideoOptions videoOptions = new VideoOptions(localVideoStreams);
    acceptCallOptions.setVideoOptions(videoOptions);
    try {
        call = incomingCall.accept(getContext(), acceptCallOptions).get();
    } catch (InterruptedException e) {
        e.printStackTrace();
    } catch (ExecutionException e) {
        e.printStackTrace();
    }

    call.addOnStateChangedListener(callStateChangeListener);
    call.addOnRemoteParticipantsUpdatedListener(participantsUpdatedListener);

    List<RemoteParticipant> remoteParticipants = call.getRemoteParticipants();
    for (RemoteParticipant remoteParticipant : remoteParticipants) {        
      remoteParticipant.addOnVideoStreamsUpdatedListener(remoteVideoStreamsUpdatedListener);
    }

    addLocalVideoStream();
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
    HangUpOptions options = new HangUpOptions();
    call.hangUp(options);
    promise.resolve(null);
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
