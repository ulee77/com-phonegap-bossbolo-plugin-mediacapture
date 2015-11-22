package com.phonegap.bossbolo.plugin.mediacapture;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;

public class BoloMediaCapture extends CordovaPlugin {
	private CallbackContext callback;
	private static final int CAPTURE_AUDIO = 0;     // Constant for capture audio
    private static final int CAPTURE_IMAGE = 1;     // Constant for capture image
    private static final int CAPTURE_VIDEO = 2;     // Constant for capture video
    private static final int CAPTURE_INVOICE = 3;	// Constant for capture invoice
	
	public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
		FakeR r = new FakeR(this.cordova.getActivity());
	}
	
	public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (action.equals("invoiceCapture")) {
        	invoiceCapture(args, callbackContext);
        } else if(action.equals("imageCapture")){
        	imageCapture(args, callbackContext);
        } else if(action.equals("videoCapture")){
        	videoCapture(args, callbackContext);
        } else if(action.equals("audioCapture")){
        	audioCapture(args, callbackContext);
        } else{
            return false;
        }
		return true;
	}
	
	public void invoiceCapture(JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callback = callbackContext;
		Intent intent = new Intent();
		intent.setClass(this.webView.getContext(), RectCameraActivity.class);
		intent.putExtra("heigth", args.getInt(0));
		intent.putExtra("width", args.getInt(1));
		this.cordova.startActivityForResult((CordovaPlugin) this, intent, CAPTURE_INVOICE);
	}
	public void imageCapture(JSONArray args, CallbackContext callbackContext){}
	public void videoCapture(JSONArray args, CallbackContext callbackContext){}
	public void audioCapture(JSONArray args, CallbackContext callbackContext){}
	
	public void dispachEvent(String type, JSONArray args){}
	
	public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
		// Result received okay
        if (resultCode == Activity.RESULT_OK) {
            // An audio clip was requested
            if (requestCode == CAPTURE_INVOICE) {
            	final String result = intent.getStringExtra("result");
                final BoloMediaCapture that = this;
                Runnable captureAudio = new Runnable() {
                    @Override
                    public void run() {
                    	that.callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                };
                this.cordova.getThreadPool().execute(captureAudio);
            }
        }
	}
}
