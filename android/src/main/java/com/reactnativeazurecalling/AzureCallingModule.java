package com.reactnativeazurecalling;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationUser;
import com.azure.android.communication.common.CommunicationUserCredential;
import com.azure.android.communication.common.PhoneNumber;
import com.azure.communication.calling.Call;
import com.azure.communication.calling.CallAgent;
import com.azure.communication.calling.CallClient;
import com.azure.communication.calling.CallState;
import com.azure.communication.calling.HangupOptions;
import com.azure.communication.calling.LocalVideoStream;
import com.azure.communication.calling.Renderer;
import com.azure.communication.calling.RenderingOptions;
import com.azure.communication.calling.ScalingMode;
import com.azure.communication.calling.StartCallOptions;
import com.azure.communication.calling.VideoDeviceInfo;
import com.azure.communication.calling.VideoOptions;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AzureCallingModule extends ReactContextBaseJavaModule {

  CallAgent callAgent;
  Call call;

  AzureCallingModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  public String getName() {
    return "AzureCalling";
  }

  @ReactMethod
  public void ping(String from, Promise promise) {
    String result = "`ping` received from " + from;
    Log.d("JavaLog", result);
    promise.resolve(result);
  }

  @ReactMethod
  public void createAgent(String userToken) {
    Context context = getReactApplicationContext().getApplicationContext();
    try {
      CommunicationUserCredential credential = new CommunicationUserCredential(userToken);
      callAgent = new CallClient().createCallAgent(context, credential).get();
    } catch (Exception ex) {
      Toast.makeText(context, "Failed to create call agent.", Toast.LENGTH_SHORT).show();
    }
  }

  @ReactMethod
  public void callACSUser(ReadableArray users) {
    Context context = getReactApplicationContext().getApplicationContext();
    ArrayList<Object> userStrings = users.toArrayList();
    CommunicationIdentifier participants[] = new CommunicationIdentifier[userStrings.size()];
    for (int i = 0; i < userStrings.size(); i++) {
      CommunicationUser acsUser = new CommunicationUser(userStrings.get(i).toString());
      participants[i] = acsUser;
    }
    StartCallOptions options = new StartCallOptions();
    call = callAgent.call(context, participants, options);
  }

  @ReactMethod
  public void hangUpCall() {
    CallState callState = call.getState();
    List<String> validStates = Arrays.asList("Connecting", "Ringing", "EarlyMedia", "Connected", "Hold");
    Log.d("CALL STATE", callState.toString());
    if (validStates.contains(callState.toString())) {
      HangupOptions options = new HangupOptions();
      call.hangup(options);
      Log.d("TRYING TO HANGUP", "hangupCall()");
    }
  }


  @ReactMethod
  public void callPSTN(String from, String to) {
    Context context = getReactApplicationContext().getApplicationContext();
    PhoneNumber callerPhone = new PhoneNumber(from);
    StartCallOptions options = new StartCallOptions();
    options.setAlternateCallerId(callerPhone);
//    options.setVideoOptions(new VideoOptions(null));
    PhoneNumber number = new PhoneNumber(to);
    call = callAgent.call(context, new PhoneNumber[] {number}, options);
  }

  @ReactMethod
  public void videoCall(String to) {
    CallClient callClient = new CallClient();
    Context appContext = getReactApplicationContext().getApplicationContext();
    VideoDeviceInfo desiredCamera = null;
    try {
      desiredCamera = callClient.getDeviceManager().get().getCameraList().get(0);
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    LocalVideoStream currentVideoStream = new LocalVideoStream(desiredCamera, appContext);
    VideoOptions videoOptions = new VideoOptions(currentVideoStream);

// Render a local preview of video so the user knows that their video is being shared
    Renderer previewRenderer = new Renderer(currentVideoStream, appContext);
    View uiView = previewRenderer.createView(new RenderingOptions(ScalingMode.Fit));
// Attach the uiView to a viewable location on the app at this point
//    layout.addView(uiView);

    CommunicationUser[] participants = new CommunicationUser[]{ new CommunicationUser(to) };
    StartCallOptions startCallOptions = new StartCallOptions();
    startCallOptions.setVideoOptions(videoOptions);
    Call call = callAgent.call(appContext, participants, startCallOptions);
  }

}
