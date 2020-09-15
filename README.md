# CS108 Barcode / RFID Reader Cordova Plugin for Android

Use the CS108 Barcode / RFID Reader Cordova Plugin to quickly develop mobile apps for barcode or RFID reading (Android). The development is based on the CS108 Java API.

## Pre-requsite

The development environment consists of the following:

- First we need node.js - Download it here. https://nodejs.org/en/download/
- Make sure to install the latest version of JDK (Java Development Kit)
- Make sure you have installed latest version of Android Studio SDK.

## Installation

1. Install **Ionic CLI** via **npm**.

    ```  
        npm install -g @ionic-cli
    ```
    
2. Install **Cordova** via **npm**.

    ```  
        npm install -g cordova
    ```
    
3. Clone or download [this source code](https://github.com/hamzahalvanaa/ionic-example-cs108) first, then navigate into the project path and install **package.json** requirements via **npm**.

    ```  
        npm install
    ```

4. Next step is have to install cordova android platform.

    ```
        ionic cordova platform add android
    ```

5. Afterwards, clone or download this plugin and install using below command via local path.

    ```
        ionic cordova plugin add <local-path-to-plugin>/cordova-plugin-cs108plugin
    ```

6. After success adding the plugin, execute the next command to build ionic and running into real device, make sure you have turn on the developer options on your android phone settings and using the same network connection on both PC and phone.

    ```
        ionic cordova run android -lc --device --no-native-run --host=0.0.0.0 --verbose
    ```

## Development Tips

1. To perform code changes of your plugin files, open cordova platform project (platforms/android) in your Android Studio IDE, change the gradle version in **gradle-wrapper.properties**.

    ```  
        distributionUrl=https\://services.gradle.org/distributions/gradle-4.10.3-all.zip
    ```

2. To prevent error and grant permissions while running the ionic project, use this commands:

    ```  
        chmod a+x platforms/android/gradlew
        chmod a+x platforms/android/cordova/build
    ```

## Supported Platforms

- Android

## Others ##
If your cordova-android version > 6.3.0, please change repositories flatDir ``` dirs 'libs'  ``` to   ```dirs 'src/main/libs'  ``` and change  dependencies ``` compile 'com.android.support:support-v4:+'   ``` to   ```compile 'com.android.support:support-v4:27.1.0'  ```in the file [cordova-plugin-cs108plugin/src/android/cs108library4a.gradle](https://github.com/hamzahalvanaa/cordova-plugin-cs108plugin/blob/master/src/android/cs108library4a.gradle)


## Example

**app.home.html**

```html
   <ion-col>
     <ion-button expand="block" size="large" color="light" (click)="connect()">
      <ion-label>Connect</ion-label>
     </ion-button>
   </ion-col>

```

**app.home.ts**

```ts
import { Component } from '@angular/core';
import { Platform } from '@ionic/angular';

declare var window: any;

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage {

  constructor(
    private platform: Platform
  ) { }

  ngOnInit() {
    this.platform.ready().then(() => {
      this.connect();
    });
  }

  connect() {
    window.plugins.cs108Plugin.connect(function (res) {
      console.log(res);
    }, function (err) {
      console.log(err);
    });
  }
}

```
