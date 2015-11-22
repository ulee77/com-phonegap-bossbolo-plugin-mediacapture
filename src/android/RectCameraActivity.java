package com.phonegap.bossbolo.plugin.mediacapture;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


public class RectCameraActivity extends Activity implements OnCaptureCallback{

	private int MASK_DEFAULT_HEIGTH = 550;
	private int MASK_DEFAULT_WIDTH = 375;
	
	private MaskSurfaceView surfaceview;
	private ImageView imageView;
//	拍照
	private Button btn_capture;
//	重拍
	private Button btn_recapture;
//	取消
	private Button btn_cancel;
//	确认
	private Button btn_ok;
	
//	拍照后得到的保存的文件路径
	private String filepath;
	
	private FakeR fakeR;
	private Intent mIntent;
	private String CANCEL_RESULT = "false";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		this.fakeR = FakeR.getInstance();
		
		this.setContentView(this.fakeR.getId("layout", "activity_rect_camera"));
		
		this.surfaceview = (MaskSurfaceView) findViewById(this.fakeR.getId("id", "surface_view"));
		this.imageView = (ImageView) findViewById(this.fakeR.getId("id", "image_view"));
		btn_capture = (Button) findViewById(this.fakeR.getId("id", "btn_capture"));
		btn_recapture = (Button) findViewById(this.fakeR.getId("id", "btn_recapture"));
		btn_ok = (Button) findViewById(this.fakeR.getId("id", "btn_ok"));
		btn_cancel = (Button) findViewById(this.fakeR.getId("id", "btn_cancel"));
		
		Bundle extras = getIntent().getExtras();    
		int heigth = extras.getInt("heigth");    
		int width = extras.getInt("width");
//		设置矩形区域大小(高，宽)
		this.surfaceview.initMask(width>0?width:MASK_DEFAULT_WIDTH, heigth>0?heigth:MASK_DEFAULT_HEIGTH, this);
		
//		拍照
		btn_capture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				btn_capture.setEnabled(false);
				btn_ok.setEnabled(true);
				btn_recapture.setEnabled(true);
//				CameraHelper.getInstance().tackPicture(RectCameraActivity.this);
			}
		});
		
//		重拍
		btn_recapture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				btn_capture.setEnabled(true);
				btn_ok.setEnabled(false);
				btn_recapture.setEnabled(false);
				imageView.setVisibility(View.GONE);
				surfaceview.setVisibility(View.VISIBLE);
				deleteFile();
				CameraHelper.getInstance().startPreview();
			}
		});
		
//		确认
		btn_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
			}
		});
		
//		取消
		btn_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				CameraHelper.getInstance().cancelScan();
				deleteFile();
				sendResult(CANCEL_RESULT);
			}
		});
	}
	
	/**
	 * 删除图片文件呢
	 */
	private void deleteFile(){
		if(this.filepath==null || this.filepath.equals("")){
			return;
		}
		File f = new File(this.filepath);
		if(f.exists()){
			f.delete();
		}
	}

	@Override
	public void onCapture(boolean success, String invoiceInfo) {
		sendResult(invoiceInfo);
	}
	
	private void sendResult(String result){
		Intent intent = new Intent();
		intent.putExtra("result", result);
		this.setResult(Activity.RESULT_OK, intent);
		RectCameraActivity.this.finish();
	}
}
