package com.reactnativeazurecalling;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.azure.android.communication.common.CommunicationUser;
import com.azure.android.communication.common.CommunicationUserCredential;
import com.azure.communication.calling.CallAgent;
import com.azure.communication.calling.CallClient;
import com.azure.communication.calling.StartCallOptions;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.ArrayList;

public class AzureCallingModule extends ReactContextBaseJavaModule {

  CallAgent callAgent;

  AzureCallingModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  public String getName() {
    return "AzureCalling";
  }

  @ReactMethod
  public void sendMessage(String to, String message, Promise promise) {
    String result = "`sendMessage` called in Java code with `to` : " + to
      + " and `message` : " + message;
    Log.d("JavaLog", result);
    promise.resolve(result);
  }

  @ReactMethod
  public void getAllPermissions() {
    Context context = getReactApplicationContext().getApplicationContext();
    String[] requiredPermissions = new String[] {
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.CAMERA,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_PHONE_STATE
    };
    ArrayList<String> permissionsToAskFor = new ArrayList<>();
    for (String permission : requiredPermissions) {
      if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
        permissionsToAskFor.add(permission);
      }
    }
    if (!permissionsToAskFor.isEmpty()) {
      ActivityCompat.requestPermissions(getCurrentActivity(), permissionsToAskFor.toArray(new String[0]), 1);
    }
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
  public void startCall(String to) {
    StartCallOptions options = new StartCallOptions();
    Context context = getReactApplicationContext().getApplicationContext();
    callAgent.call(context, new CommunicationUser[] {new CommunicationUser(to)}, options);
  }
}
