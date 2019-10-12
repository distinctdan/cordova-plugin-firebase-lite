package org.apache.cordova.firebase;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.perf.FirebasePerformance;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.fabric.sdk.android.Fabric;


public class FirebasePlugin extends CordovaPlugin {

    protected static FirebasePlugin instance = null;
    private FirebaseAnalytics mFirebaseAnalytics;
    private static CordovaInterface cordovaInterface = null;
    private static Context applicationContext = null;
    private static Activity cordovaActivity = null;
    protected static final String TAG = "FirebasePlugin";
    protected static final String KEY = "badge";

    @Override
    protected void pluginInitialize() {
        instance = this;
        cordovaActivity = this.cordova.getActivity();
        applicationContext = cordovaActivity.getApplicationContext();
        final Bundle extras = cordovaActivity.getIntent().getExtras();
        FirebasePlugin.cordovaInterface = this.cordova;
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "Starting Firebase plugin");
                    FirebaseApp.initializeApp(applicationContext);
                    mFirebaseAnalytics = FirebaseAnalytics.getInstance(applicationContext);
                }catch (Exception e){
                    handleExceptionWithoutContext(e);
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try{
            if (action.equals("getId")) {
                this.getId(callbackContext);
                return true;
            } else if (action.equals("logEvent")) {
                this.logEvent(callbackContext, args.getString(0), args.getJSONObject(1));
                return true;
            } else if (action.equals("logError")) {
                this.logError(callbackContext, args);
                return true;
            }else if(action.equals("setCrashlyticsUserId")){
                this.setCrashlyticsUserId(callbackContext, args.getString(0));
                return true;
            } else if (action.equals("setScreenName")) {
                this.setScreenName(callbackContext, args.getString(0));
                return true;
            } else if (action.equals("setUserId")) {
                this.setUserId(callbackContext, args.getString(0));
                return true;
            } else if (action.equals("setAnalyticsCollectionEnabled")) {
                this.setAnalyticsCollectionEnabled(callbackContext, args.getBoolean(0));
                return true;
            } else if (action.equals("setPerformanceCollectionEnabled")) {
                this.setPerformanceCollectionEnabled(callbackContext, args.getBoolean(0));
                return true;
            } else if (action.equals("setCrashlyticsCollectionEnabled")) {
                this.setCrashlyticsCollectionEnabled(callbackContext);
                return true;
            } else if (action.equals("logMessage")) {
                logMessage(args, callbackContext);
                return true;
            } else if (action.equals("sendCrash")) {
                sendCrash(args, callbackContext);
                return true;
            }
        }catch(Exception e){
            handleExceptionWithContext(e, callbackContext);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        instance = null;
        cordovaActivity = null;
        cordovaInterface = null;
        applicationContext = null;
        super.onDestroy();
    }

    /**
     * Get a string from resources without importing the .R package
     *
     * @param name Resource Name
     * @return Resource
     */
    private String getStringResource(String name) {
        return applicationContext.getString(
                applicationContext.getResources().getIdentifier(
                        name, "string", applicationContext.getPackageName()
                )
        );
    }

    private void getId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    String id = FirebaseInstanceId.getInstance().getId();
                    callbackContext.success(id);
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                }
            }
        });
    }

    private void logEvent(final CallbackContext callbackContext, final String name, final JSONObject params)
            throws JSONException {
        final Bundle bundle = new Bundle();
        try {
            Iterator iter = params.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                Object value = params.get(key);

                if (value instanceof Integer || value instanceof Double) {
                    bundle.putFloat(key, ((Number) value).floatValue());
                } else {
                    bundle.putString(key, value.toString());
                }
            }
        } catch (Exception e) {
            handleExceptionWithoutContext(e);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.logEvent(name, bundle);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                }
            }
        });
    }

    private void logError(final CallbackContext callbackContext, final JSONArray args) throws JSONException {
        String message = args.getString(0);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    // We can optionally be passed a stack trace generated by stacktrace.js.
                    if (args.length() == 2) {
                        Crashlytics.logException(new JavaScriptException(message, args.getJSONArray(1)));
                    } else {
                        Crashlytics.logException(new JavaScriptException(message));
                    }

                    Log.e(TAG, message);
                    callbackContext.success(1);
                } catch (Exception e) {
                    Crashlytics.log(Log.ERROR, TAG, "logError errored. Orig error: " + message);
                    Crashlytics.logException(e);
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void logMessage(final JSONArray data,
                        final CallbackContext callbackContext) {

        String message = data.optString(0);
        Crashlytics.log(message);
        callbackContext.success();
    }

    private void sendCrash(final JSONArray data,
						   final CallbackContext callbackContext) {

        cordovaActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				throw new RuntimeException("This is a crash");
			}
		});
	}


    private void setCrashlyticsUserId(final CallbackContext callbackContext, final String userId) {
        cordovaActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Crashlytics.setUserIdentifier(userId);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                }
            }
        });
    }

    private void setScreenName(final CallbackContext callbackContext, final String name) {
        // This must be called on the main thread
        cordovaActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.setCurrentScreen(cordovaActivity, name, null);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                }
            }
        });
    }

    private void setUserId(final CallbackContext callbackContext, final String id) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.setUserId(id);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                }
            }
        });
    }

    private void setAnalyticsCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                    e.printStackTrace();
                }
            }
        });
    }

    private void setPerformanceCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enabled);
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                    e.printStackTrace();
                }
            }
        });
    }

    private void setCrashlyticsCollectionEnabled(final CallbackContext callbackContext) {
        final FirebasePlugin self = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Fabric.with(self.applicationContext, new Crashlytics());
                    callbackContext.success();
                } catch (Exception e) {
                    handleExceptionWithContext(e, callbackContext);
                    e.printStackTrace();
                }
            }
        });
    }

    protected static void handleExceptionWithContext(Exception e, CallbackContext context){
        String msg = e.toString();
        Log.e(TAG, msg);
        Crashlytics.log(msg);
        context.error(msg);
    }

    protected static void handleExceptionWithoutContext(Exception e){
        String msg = e.toString();
        Log.e(TAG, msg);
        Crashlytics.log(msg);
    }

    private void executeGlobalJavascript(final String jsString){
        if(cordovaActivity == null) return;
        cordovaActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:" + jsString);
            }
        });
    }
}
