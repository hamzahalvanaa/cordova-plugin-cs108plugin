# CS108 RFID Reader Cordova Plugin for Android (In Development)

Use the CS108 RFID Reader Cordova Plugin to quickly develop mobile apps for RFID reading (Android). The development is based on the CS108 Java API.

## Pre-requsite

The development environment consists of the following:

- First we need node.js - Download it here. https://nodejs.org/en/download/
- Make sure to install the latest version of JDK (Java Development Kit)
- Make sure you have installed latest version of Android Studio SDK

## Basic Installation

1. Install cordova android platform **Recommended version is 8.x.x**.

    ```
        cordova platform add android@8
    ```

2. Afterwards, clone or download this plugin and install using below command via local path.

    ```
        cordova plugin add https://github.com/hamzahalvanaa/cordova-plugin-cs108plugin.git
    ```

3. You need to configure plugin to work by replace all of your current project MainActivity.java file with the file on plugin. So it's should be located in {{your-app}}/platforms/android/app/src/main/java/{{path-to-your-mainactivity-based-on-your-app-package-name}}. The plugin MainActivity file is located in https://github.com/hamzahalvanaa/cordova-plugin-cs108plugin/tree/master/src/android/java/MainActivity.java.

4. After success adding the plugin, execute the next command to build running into real device, make sure you have turn on the developer options on your android phone settings and using the same network connection on both PC and phone.

    ```
        cordova run android
    ```

## Development Tips

1. To perform code changes of your plugin files, open cordova platform project (platforms/android) in your Android Studio IDE, change the gradle version in **gradle-wrapper.properties**.

    ```  
        distributionUrl=https\://services.gradle.org/distributions/gradle-4.10.3-all.zip
    ```

2. To prevent error and grant permissions while running the project, use this commands:

    ```  
        chmod a+x platforms/android/gradlew
        chmod a+x platforms/android/cordova/build
    ```

## Supported Platforms

- Android

## Others ##
If your cordova-android version > 6.3.0, please change repositories flatDir ``` dirs 'libs'  ``` to   ```dirs 'src/main/libs'  ``` and change  dependencies ``` compile 'com.android.support:support-v4:+'   ``` to   ```compile 'com.android.support:support-v4:27.1.0'  ```in the file [cordova-plugin-cs108plugin/src/android/cs108library4a.gradle](https://github.com/hamzahalvanaa/cordova-plugin-cs108plugin/blob/master/src/android/cs108library4a.gradle)


## Example

For the project example, you can refer to this project, https://github.com/hamzahalvanaa/ionic-example-cs108.git.