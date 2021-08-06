# Pusher Flutter client

An unofficial Flutter plugin that wraps [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java) on Android and [pusher-websocket-swift](https://github.com/pusher/pusher-websocket-swift) on iOS.

This package lets you consume events from a Pusher server. In order to use this library, you need to have an account on <http://pusher.com>. After registering, you will need the application credentials for your app.

*Note*: This plugin is still under development, and some APIs might not be available yet. [Feedback](https://github.com/amanzdev/flutter_pusher/issues) and [Pull Requests](https://github.com/amanzdev/flutter_pusher/pulls) are most welcome!

## Getting Started

```dart
import 'package:pusher/pusher.dart';

void main() {

  var options = PusherOptions(host: '10.0.2.2', port: 6001, encrypted: false);
  FlutterPusher pusher = Pusher('app', options, enableLogging: true);

  pusher.subscribe('channel').bind('event', (event) => {});
}
```

### Lazy Connect

Connection to the server can be delayed, so set the **lazyConnect** prop on the client constructor.

##### R8/Proguard code obfuscation

If you have enabled code obfuscation with R8 or proguard, you need to add the following rule.

`android/app/build.gradle`:

```groovy
buildTypes {
  release {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
  }
}
```

`android/app/proguard-rules.pro`:

```
-keep class my.amanz.pusher.flutter_pusher.** { *; }
```

## Development
Generate the models and the factories: `flutter packages pub run build_runner build --delete-conflicting-outputs`

