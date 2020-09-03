var exec = require('cordova/exec');

var PLUGIN_NAME = 'CS108Plugin';

// Empty constructor
function CS108Plugin() { }

// The function that passes work along to native shells
CS108Plugin.prototype.show = function (message, duration, successCallback, errorCallback) {
  var options = {};
  options.message = message;
  options.duration = duration;
  exec(successCallback, errorCallback, PLUGIN_NAME, 'show', [options]);
};

CS108Plugin.prototype.connect = function (successCallback, errorCallback) {
  exec(successCallback, errorCallback, PLUGIN_NAME, 'connect', []);
};

// Installation constructor that binds CS108Plugin to window
CS108Plugin.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.cs108Plugin = new CS108Plugin();
  return window.plugins.cs108Plugin;
};
cordova.addConstructor(CS108Plugin.install);