package com.phonegap.bossbolo.plugin.mediacapture;

import android.app.Activity;
import android.content.Context;

/**
 * 读取主应用包中的R文件
 *
 * ([^.\w])R\.(\w+)\.(\w+)
 * $1fakeR("$2", "$3")
 *
 * @author Maciej Nux Jaros
 */
public class FakeR {
	private Context context;
	private String packageName;
	private static FakeR instance;

	public FakeR(Activity activity) {
		context = activity.getApplicationContext();
		packageName = context.getPackageName();
		instance = this;
	}
	
	public FakeR(String packageName) { 
		this.packageName = packageName;
		instance = this;
	}

	public FakeR(Context context) {
		this.context = context;
		packageName = context.getPackageName();
		instance = this;
	}
	
	public static FakeR getInstance (){
        return instance;
	}

	public int getId(String group, String key) {
		return context.getResources().getIdentifier(key, group, packageName);
	}

	public static int getId(Context context, String group, String key) {
		return context.getResources().getIdentifier(key, group, context.getPackageName());
	}
}
