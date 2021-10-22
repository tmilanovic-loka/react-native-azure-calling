package com.reactnativeazurecalling;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.VideoView;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class LocalVideoViewManager extends SimpleViewManager<View> {
  public static final String REACT_CLASS = "LocalVideoView";

  private static View view = null;
  
  public static View GetView()
  {
    return view;
  }

  public LocalVideoViewManager(ReactApplicationContext context) {
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected View createViewInstance(ThemedReactContext context)
  {
      view = new LinearLayout(context);
      view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
      return view;
  }
}
