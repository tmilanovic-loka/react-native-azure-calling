/* eslint-disable react-native/no-inline-styles */
import * as React from 'react';
import {
  StyleSheet,
  View,
  PermissionsAndroid,
  Dimensions,
  StatusBar,
  TouchableOpacity,
  Text,
  ButtonProps,
  ImageBackground,
  TextProps,
} from 'react-native';
import AzureCalling from 'react-native-azure-calling';
import Config from '../config.json';

const win = Dimensions.get('window');
const TOKEN = Config.TOKEN;

const onPress = async () => {
  let result = await AzureCalling.sendMessage(
    'John Doe',
    'Sending a test message your way'
  );
  console.log(result);
};

const getPermissions = async () => {
  let permissions = [
    PermissionsAndroid.PERMISSIONS.CAMERA,
    PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
    PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
    PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
  ];
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
};

const testCall = async () => {
  AzureCalling.createAgent(TOKEN);
  AzureCalling.startCall('8:echo123');
};

export default function App() {
  return (
    <View style={{ backgroundColor: 'white', flex: 1 }}>
      <ImageBackground
        source={require('./assets/images/bg-gradient.png')}
        style={{ flex: 1 }}
      >
        <StatusBar
          backgroundColor={COLOR_GRADIENT('0.8')}
          barStyle="dark-content"
        />

        <View
          style={{
            alignSelf: 'center',
            padding: 32,
            width: win.width,
          }}
        >
          <H1 style={{ textAlign: 'center', fontSize: 24 }}>
            React Native Azure Calling
          </H1>
          <H1 style={{ textAlign: 'center', opacity: 0.5 }}>Example App</H1>
        </View>

        <View style={{ marginLeft: 16, marginRight: 16, marginTop: 16 }}>
          <View />
          <View
            style={{
              width: win.width - 96,
              alignSelf: 'center',
            }}
          >
            <View style={{ marginTop: 16, marginBottom: 8 }}>
              <ButtonPrimary title="Test Library Binding" onPress={onPress} />
              <ButtonPrimary title="Get Permissions" onPress={getPermissions} />
              <ButtonPrimary title="Make Test Voice Call" onPress={testCall} />
            </View>
          </View>
        </View>
      </ImageBackground>
    </View>
  );
}

const COLOR_PRIMARY = '#399c7d';
const COLOR_GRADIENT = (alpha: string) => `rgba(243, 233, 210, ${alpha})`;

const styles = StyleSheet.create({
  buttonText: {
    color: 'white',
    fontSize: 16,
    textAlign: 'center',
    textAlignVertical: 'center',
    height: 48,
  },
  buttonDefault: {
    elevation: 4,
    borderRadius: 2,
    height: 48,
    marginTop: 8,
    marginBottom: 8,
  },
  buttonPrimary: {
    width: '100%',
    alignSelf: 'center',
    backgroundColor: COLOR_PRIMARY,
  },
  disabled: {
    backgroundColor: '#aaa',
    elevation: 0,
  },
});

class ButtonPrimary extends React.Component<ButtonProps> {
  constructor(props: ButtonProps) {
    super(props);
  }

  render() {
    return (
      <TouchableOpacity
        style={[styles.buttonDefault, styles.buttonPrimary]}
        onPress={this.props.onPress}
      >
        <Text style={styles.buttonText}>
          {this.props.children || this.props.title}
        </Text>
      </TouchableOpacity>
    );
  }
}

class H1 extends React.Component<TextProps> {
  render() {
    return (
      <Text
        style={[
          {
            fontFamily: 'Montserrat-SemiBold',
            color: '#333',
            fontSize: 20,
            marginTop: 2,
            marginBottom: 2,
            lineHeight: 32,
          },
          this.props.style,
        ]}
      >
        {this.props.children}
      </Text>
    );
  }
}
