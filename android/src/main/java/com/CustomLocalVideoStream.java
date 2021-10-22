package com.reactnativeazurecalling;

import com.azure.android.communication.calling.*;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.nio.ByteBuffer;import java.util.concurrent.ExecutionException;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class CustomLocalVideoStream {

  private final String VirtualDeviceName = "RamenVirtualVideoDevice";

  private DeviceManager callDeviceManager = null;
  private Context context = null;
  private OutboundVirtualVideoDevice outboundVirtualVideoDevice;
  private MediaFrameSender mediaFrameSender;
  private VideoOptions localVideoOptions = null;
  private boolean sendingVideoFrame = false;
  private ByteBuffer outVideoBuffer = null;
  private HandlerThread helperThread = null;
  private Handler helperThreadHandler = null;

  CustomLocalVideoStream(Context appContext, DeviceManager deviceManager) {
    context = appContext;
    callDeviceManager = deviceManager;
    createOutboundVirtualVideoDevice();
  }

  VideoOptions getVideoOptions() {
      return localVideoOptions;
  }

  private void createOutboundVirtualVideoDevice() {
    VirtualDeviceIdentification deviceId = new VirtualDeviceIdentification();
    deviceId.setId(VirtualDeviceName);
    deviceId.setName(VirtualDeviceName);
    final int formatWidth = 1280;
    final int formatHeight = 720;
    final int desiredFps = 30;

    VideoFormat format1 = new VideoFormat();
    format1.setWidth(formatWidth);
    format1.setHeight(formatHeight);
    format1.setPixelFormat(PixelFormat.BGRX);
    format1.setMediaFrameKind(MediaFrameKind.VIDEO_SOFTWARE);
    format1.setFramesPerSecond(desiredFps);
    format1.setStride1(formatWidth * 4);

    /*VideoFormat format2 = new VideoFormat();
    format2.setWidth(formatWidth);
    format2.setHeight(formatHeight);
    format2.setPixelFormat(PixelFormat.RGBA);
    format2.setMediaFrameKind(MediaFrameKind.VIDEO_HARDWARE);
    format2.setFramesPerSecond(desiredFps);
    format2.setStride1(formatHeight * 4);*/

    sendingVideoFrame = false;
    helperThread = new HandlerThread("SendFrame");
    helperThread.start();
    helperThreadHandler = new Handler(helperThread.getLooper());

    OutboundVirtualVideoDeviceOptions options = new OutboundVirtualVideoDeviceOptions();
    options.setDeviceIdentification(deviceId);
    options.setVideoFormats(new VideoFormat[] { format1 });
    options.addOnFlowChangedListener(virtualDeviceFlowControlArgs -> {      
      mediaFrameSender = virtualDeviceFlowControlArgs.getMediaFrameSender();
      Log.i("Native/VideoEvent", "VirtualDevice: OnFlowChanged: " + mediaFrameSender.getRunningState());
      if (mediaFrameSender.getRunningState() == VirtualDeviceRunningState.STARTED) {
        sendingVideoFrame = true;
        helperThreadHandler.post(() -> {
          sendFrame();
        });
      } else {
        sendingVideoFrame = false;
      }
    });

    try {
      outboundVirtualVideoDevice = callDeviceManager.createOutboundVirtualVideoDevice(options).get();
      if (outboundVirtualVideoDevice != null) {
        Log.i("Native/VideoEvent", "Created Virtual Video Device: " + outboundVirtualVideoDevice.getRunningState());
      } else {
        Log.i("Native/VideoEvent", "Failed to create Virtual Video Device");
      }
  
      Thread.sleep(2000);
      localVideoOptions = getOutboundVirtualVideoDeviceVideoOptions();      
      
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }    
  }

  private synchronized void sendFrame() {
    Random rand = new Random();
    
    while (sendingVideoFrame)
    {
      if (mediaFrameSender.getMediaFrameKind() == MediaFrameKind.VIDEO_SOFTWARE) {
        Log.i("Native/VideoEvent", "Frame requested. Generating Frame...");
        SoftwareBasedVideoFrame sender = (SoftwareBasedVideoFrame)mediaFrameSender;
        //int delayBetweenFrames = (int)(1000.0 / sender.getVideoFormat().getFramesPerSecond());
        int w = sender.getVideoFormat().getWidth();
        int h = sender.getVideoFormat().getHeight();
        int bufferSize = (w * h) * 4;            

        outVideoBuffer = ByteBuffer.allocate(bufferSize);
        if (outVideoBuffer != null)
        {
          outVideoBuffer.order(ByteOrder.nativeOrder());
          Log.i("Native/VideoEvent", "Allocated buffer with size: " + bufferSize);

          while (sendingVideoFrame) {
           // byte r = (byte)rand.nextInt(256);
           // byte g = (byte)rand.nextInt(256);
            //byte b = (byte)rand.nextInt(256);
            //outVideoBuffer.rewind();

            /*for (int y = 0; y < h; ++y) {
              for (int x = 0; x < w * 4; x += 4)
              {
                outVideoBuffer.put((w * 4 * y) + x, (byte)(y % b)); //b
                outVideoBuffer.put((w * 4 * y) + x + 1, (byte)(y % g)); //g
                outVideoBuffer.put((w * 4 * y) + x + 2, (byte)(y % r)); //r
                outVideoBuffer.put((w * 4 * y) + x + 3, (byte)0);
              }
            }*/

            try {
              int timeStamp = mediaFrameSender.getTimestamp();
              Log.i("Native/VideoEvent", "Sending frame in state: " + mediaFrameSender.getRunningState());
              FrameConfirmation fr = sender.sendFrame(outVideoBuffer, timeStamp).get();
              Log.i("Native/VideoEvent", "Sent frame with status: " + fr.getStatus());
              //Thread.sleep(delayBetweenFrames);
            } catch (InterruptedException e) {
              e.printStackTrace();
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }    
  }

  private VideoOptions getOutboundVirtualVideoDeviceVideoOptions() {
    for (VideoDeviceInfo videoDeviceInfo : callDeviceManager.getCameras()) {
      String deviceId = videoDeviceInfo.getId();
      Log.i("Native/VideoEvent", "Found camera: " + deviceId);
      if (deviceId.equalsIgnoreCase(VirtualDeviceName)) {
        VideoOptions videoOptions = new VideoOptions(new LocalVideoStream[] {
          new LocalVideoStream(videoDeviceInfo, context)
        });
        return videoOptions;
      }
    }
    return null;
  }
}
