var exec = require('cordova/exec');

var PLUGIN_NAME = 'CS108Plugin';

// Empty constructor
function CS108Plugin() { }

// The function that passes work along to native shells
CS108Plugin.prototype.showToast = function (message, duration, successCallback, errorCallback) {
    var options = {};
    options.message = message;
    options.duration = duration;
    exec(successCallback, errorCallback, PLUGIN_NAME, 'showToast', [options]);
};

CS108Plugin.prototype.startFindDevices = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'startFindDevices', []);
};

CS108Plugin.prototype.stopFindDevices = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'stopFindDevices', []);
};

CS108Plugin.prototype.getDeviceInfo = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'getDeviceInfo', []);
};

CS108Plugin.prototype.connect = function (device, successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'connect', [device]);
};

CS108Plugin.prototype.disconnect = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'disconnect', []);
};

CS108Plugin.prototype.beginInventory = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'beginInventory', []);
};

CS108Plugin.prototype.startScan = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'startScan', []);
};

CS108Plugin.prototype.stopScan = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'stopScan', []);
};

CS108Plugin.prototype.clearTagList = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, 'clearTagList', []);
};

CS108Plugin.prototype.writeTag = function (tagList, newTag, successCallback, errorCallback) {
    var writeParams = {};
    writeParams.tagList = tagList;
    writeParams.newTag = newTag;
    exec(successCallback, errorCallback, PLUGIN_NAME, 'writeTag', [writeParams]);
};

// Installation constructor that binds CS108Plugin to window
CS108Plugin.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.CS108Plugin = new CS108Plugin();
    return window.plugins.CS108Plugin;
};
cordova.addConstructor(CS108Plugin.install);
