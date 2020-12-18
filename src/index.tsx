import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';

type AzureCallingType = {
  ping(from: string): Promise<string>;
  createAgent(token: string): Promise<null>;
  callACSUsers(to: Array<String>): Promise<null>;
  callPSTN(from: string, to: string): Promise<null>;
  hangUpCall(): Promise<null>;
  addEventListener(
    eventName: string,
    callback: (...args: any[]) => any
  ): EmitterSubscription;
};

const { AzureCalling } = NativeModules;

AzureCalling.addEventListener = (
  eventName: string,
  callback: (...args: any[]) => any
): EmitterSubscription => {
  const eventEmitter = new NativeEventEmitter(AzureCalling);
  return eventEmitter.addListener(eventName, callback);
};

export default AzureCalling as AzureCallingType;
