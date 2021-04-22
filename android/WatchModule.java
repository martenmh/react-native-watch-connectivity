package com.canvasheroes.ommetje;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactMethod;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

/*
  ArrayUtil exposes a set of helper methods for working with
  ReadableArray (by React Native), Object[], and JSONArray.

  MIT License

  Copyright (c) 2020 Marc Mendiola

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */
class BridgeUtil {
    public static JSONObject toJSONObject(ReadableMap readableMap) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();

        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType type = readableMap.getType(key);

            switch (type) {
                case Null:
                    jsonObject.put(key, null);
                    break;
                case Boolean:
                    jsonObject.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    jsonObject.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    jsonObject.put(key, readableMap.getString(key));
                    break;
                case Map:
                    jsonObject.put(key, toJSONObject(readableMap.getMap(key)));
                    break;
                case Array:
                    jsonObject.put(key, toJSONArray(readableMap.getArray(key)));
                    break;
            }
        }

        return jsonObject;
    }

    public static JSONArray toJSONArray(ReadableArray readableArray) throws JSONException {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < readableArray.size(); i++) {
            ReadableType type = readableArray.getType(i);

            switch (type) {
                case Null:
                    jsonArray.put(i, null);
                    break;
                case Boolean:
                    jsonArray.put(i, readableArray.getBoolean(i));
                    break;
                case Number:
                    jsonArray.put(i, readableArray.getDouble(i));
                    break;
                case String:
                    jsonArray.put(i, readableArray.getString(i));
                    break;
                case Map:
                    jsonArray.put(i, toJSONObject(readableArray.getMap(i)));
                    break;
                case Array:
                    jsonArray.put(i, toJSONArray(readableArray.getArray(i)));
                    break;
            }
        }

        return jsonArray;
    }
}

class AppPackage implements ReactPackage {
    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(
            new RNWatch(reactContext)
        );
    }
}

/**
 * Native interface for React Native used for interacting with the Smart Watch,
 * These functions are used by the react-native-watch-connectivity library
 * Note, the following types are used based on the necessary JS types:
 * Java  -- JavaScript
 * Boolean -> Bool
 * Integer -> Number
 * Double -> Number
 * Float -> Number
 * String -> String
 * Callback -> function
 * ReadableMap -> Object
 * ReadableArray -> Array
 */
class RNWatch extends ReactContextBaseJavaModule implements RCTEventEmitter {
    static String EVENT_FILE_TRANSFER = "WatchFileTransfer";
    static String EVENT_RECEIVE_MESSAGE = "WatchReceiveMessage";
    static String EVENT_RECEIVE_MESSAGE_DATA = "WatchReceiveMessageData";
    static String EVENT_ACTIVATION_ERROR = "WatchActivationError";
    static String EVENT_WATCH_REACHABILITY_CHANGED = "WatchReachabilityChanged";
    static String EVENT_WATCH_USER_INFO_RECEIVED = "WatchUserInfoReceived";
    static String EVENT_APPLICATION_CONTEXT_RECEIVED = "WatchApplicationContextReceived";
    static String EVENT_SESSION_DID_DEACTIVATE = "WatchSessionDidDeactivate";
    static String EVENT_SESSION_BECAME_INACTIVE = "WatchSessionBecameInactive";
    static String EVENT_PAIR_STATUS_CHANGED = "WatchPairStatusChanged";
    static String EVENT_INSTALL_STATUS_CHANGED = "WatchInstallStatusChanged";

    public String TAG = "RNWatch";

    /* Messaging Service Connection code */
    Intent messageServiceIntent;
    Boolean isServiceBound = false;
    MessageService boundService;
    MessageServiceConnection serviceConnection;

    private class MessageServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MessageService.MessageBinder myBinder = (MessageService.MessageBinder) service;
            boundService = myBinder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }

    }

    public RNWatch(ReactApplicationContext reactContext) {
        super(reactContext);
        serviceConnection = new MessageServiceConnection();

        messageServiceIntent = new Intent(reactContext, MessageService.class);
        reactContext.startService(messageServiceIntent);
        reactContext.bindService(messageServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @NotNull
    @Override
    public String getName() {
        return "RNWatch";
    }

    @ReactMethod
    public void sendMessage(ReadableMap message, Callback replyCallback, Callback errCallback) throws JSONException {
        JSONObject json = BridgeUtil.toJSONObject(message);
        Log.w(TAG, json.toString());

        boundService.sendMessageToAll(json.toString().getBytes());
    }

    @ReactMethod
    public void sendMessageData(ReadableMap data, Number encoding, Callback replyCallback, Callback errCallback) {

    }

    @ReactMethod
    public String getPlatform() {
        return "wearos";
    }

    @ReactMethod
    public Boolean getIsPaired() {
        return boundService.isConnected();
    }

    @ReactMethod
    public boolean getReachability() {
        Log.d(TAG, "getReachability");
        return true;
    }

    @ReactMethod
    public void replyToMessageWithId() {
        Log.d(TAG, "replyToMessageWithId");
    }

    @ReactMethod
    public boolean getIsWatchAppInstalled() {
        Log.d(TAG, "getIsWatchAppInstalled");
        return true;
    }

    @ReactMethod
    public void getApplicationContext() {
        Log.d(TAG, "getApplicationContext");
    }

    @ReactMethod
    public void transferFile() {
        Log.d(TAG, "transferFile");
    }

    @ReactMethod
    public void getFileTransfers() {
        Log.d(TAG, "getFileTransfers");
    }

    @ReactMethod
    public void dequeueUserInfo() {
        Log.d(TAG, "dequeueUserInfo");
    }

    @Override
    public void receiveEvent(int targetTag, String eventName, @Nullable WritableMap event) {
    }

    @Override
    public void receiveTouches(String eventName, WritableArray touches, WritableArray changedIndices) {

    }

}
