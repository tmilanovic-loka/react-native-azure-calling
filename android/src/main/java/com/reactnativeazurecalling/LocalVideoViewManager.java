package com.reactnativeazurecalling;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.VideoView;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class LocalVideoViewManager extends ViewGroupManager<LocalVideoView> {
  public static final String REACT_CLASS = "LocalVideoView";

  private ReactApplicationContext applicationContext;
  private static LocalVideoView view = null;
  
  public static LocalVideoView GetView()
  {
    return view;
  }

  public LocalVideoViewManager(ReactApplicationContext context) {
    super();
    this.applicationContext = context;
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected LocalVideoView createViewInstance(ThemedReactContext context)
  {
    view = new LocalVideoView(context, applicationContext);
    return view;
  }

  @Override
  public boolean needsCustomLayoutForChildren() {
      return true;
  }
}
