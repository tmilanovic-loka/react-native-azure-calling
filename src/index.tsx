import { NativeModules } from 'react-native';

type AzureCallingType = {
  ping(from: string): Promise<string>;
  createAgent(token: string): Promise<null>;
  callACSUsers(to: Array<String>): Promise<null>;
  callPSTN(from: string, to: string): Promise<null>;
  hangUpCall(): Promise<null>;
};

const { AzureCalling } = NativeModules;

export default AzureCalling as AzureCallingType;
