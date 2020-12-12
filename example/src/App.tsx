import * as React from 'react';
import { StyleSheet, View, Button, PermissionsAndroid } from 'react-native';
import AzureCalling from 'react-native-azure-calling';
import Config from '../config.json';

const TOKEN = Config.TOKEN;

const onPress = async () => {
  let result = await AzureCalling.sendMessage('John Doe', 'Sending a test message your way');
  console.log(result);
};

const getAllPermissions = async () => {
  let permissions = [
    PermissionsAndroid.PERMISSIONS.CAMERA,
    PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
    PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
    PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,    
  ]
  try {
    for (const perm of permissions) {
      const granted = await PermissionsAndroid.request(perm);  
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log(`${perm} Granted`);
      } else {
        console.log(`${perm} Denied`);
      }
    }
  } catch (err) {
    console.warn(err);
  }
}

const testCall = async () => {
  AzureCalling.createAgent(TOKEN);
  AzureCalling.startCall('8:echo123');
}

export default function App() {
  
  return (
    <View style={styles.container}>
    
    <View style={styles.sectionContainer}>
    <Button
    title="Click to Send Message"
    color="#841584"
    onPress={onPress}
    />
    </View>
    <View style={styles.sectionContainer}>
    <Button
    title="Click to Get Permissions"
    color="olive"
    onPress={getAllPermissions}
    />
    </View>
    <View style={styles.sectionContainer}>
    <Button
    title="Click to Test Call"
    color="firebrick"
    onPress={testCall}
    />
    </View>
    </View>
    );
  }
  
  const styles = StyleSheet.create({
    container: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
    },
    sectionContainer: {
      marginTop: 32,
      paddingHorizontal: 24,
    },
  });
  