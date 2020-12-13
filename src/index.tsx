import { NativeModules } from 'react-native';

type AzureCallingType = {
  sendMessage(to: string, message: string): Promise<string>;
  createAgent(token: string): Promise<null>;
  startCall(to: string): Promise<null>;
  callACSUser(to: Array<String>): Promise<null>;
  hangUpCall(): Promise<null>;
};

const { AzureCalling } = NativeModules;

export default AzureCalling as AzureCallingType;
