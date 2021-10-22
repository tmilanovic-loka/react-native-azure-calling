import React from 'react';
import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
  requireNativeComponent,
  ViewStyle,
  ViewProps,
} from 'react-native';

const { AzureCalling } = NativeModules;

type AzureCallingType = {
  ping(from: string): Promise<string>;
  createAgent(token: string): Promise<null>;
  startGroupCall(): Promise<null>;
  joinGroupCall(token: string): Promise<null>;
  callACSUsers(to: Array<String>): Promise<null>;
  callPSTN(from: string, to: string): Promise<null>;
  hangUpCall(): Promise<null>;
  addCallStateListener: IaddCallStateListener;
  addTestView(): Promise<null>;
};

interface IaddCallStateListener {
  (callback: (event: { callState: string }) => any): EmitterSubscription;
}

const addCallStateListener: IaddCallStateListener = (callback) => {
  const eventEmitter = new NativeEventEmitter(AzureCalling);
  return eventEmitter.addListener('CALL_STATE_CHANGED', callback);
};

AzureCalling.addCallStateListener = addCallStateListener;

type LocalVideoViewProps = {
  style?: ViewStyle;
};

const LocalVideoViewRaw = requireNativeComponent<LocalVideoViewProps>('LocalVideoView');
type LocalVideoProps = ViewProps & LocalVideoViewProps;

export const LocalVideoView : React.FC<LocalVideoProps> = (props) => {
    const style = {...(props.style as object)};
    return <LocalVideoViewRaw style={style} />
};

type RemoteVideoViewProps = {
  style?: ViewStyle;
};

const RemoteVideoViewRaw = requireNativeComponent<RemoteVideoViewProps>('RemoteVideoView');
type RemoteVideoProps = ViewProps & RemoteVideoViewProps;

export const RemoteVideoView : React.FC<RemoteVideoProps> = (props) => {
    const style = {...(props.style as object)};
    return <RemoteVideoViewRaw style={style} />
};

export default AzureCalling as AzureCallingType;
