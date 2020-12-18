import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';

const { AzureCalling } = NativeModules;

type AzureCallingType = {
  ping(from: string): Promise<string>;
  createAgent(token: string): Promise<null>;
  callACSUsers(to: Array<String>): Promise<null>;
  callPSTN(from: string, to: string): Promise<null>;
  hangUpCall(): Promise<null>;
  addCallStateListener: IaddCallStateListener;
};

interface IaddCallStateListener {
  (callback: (event: { callState: string }) => any): EmitterSubscription;
}

const addCallStateListener: IaddCallStateListener = (callback) => {
  const eventEmitter = new NativeEventEmitter(AzureCalling);
  return eventEmitter.addListener('CALL_STATE_CHANGED', callback);
};

AzureCalling.addCallStateListener = addCallStateListener;

export default AzureCalling as AzureCallingType;
