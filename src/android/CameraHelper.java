package com.phonegap.bossbolo.plugin.mediacapture;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//import com.googlecode.tesseract.android.TessBaseAPI;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

@SuppressWarnings("deprecation")
public class CameraHelper {
	private final String TAG = "CameraHelper";
	private ToneGenerator tone;
	private String filePath;// = "/carchecker/photo";
	private boolean isPreviewing;
	
	private static CameraHelper helper;
	private Camera camera;
	private MaskSurfaceView surfaceView;
	
	
//	private TessBaseAPI baseApi;
	private String datapath;
	private String language = "eng";

	private OnCaptureCallback ca;
	private Bitmap captureBitmap;
	private Boolean decoding = false;	//是否正在解析拍照数据
	private int AUTO_FOCUS_DELAY = 2000; //自动对焦间隔
	private int CAMERA_STATE_DELAY = 3000; //默认在相机界面停留时间，不包含拍照数据解析时间
	private static long firstTime;		//首次对焦时间
	private SleepTask st;
	private static int stId = 0;
//	分辨率
	private Size resolution;
	
//	照片质量
	private int picQuality = 100;
	
//	照片尺寸
	private Size pictureSize;
	
//	闪光灯模式(default：自动)
	private String flashlightStatus = Camera.Parameters.FLASH_MODE_OFF;
	
	public enum Flashlight{
		AUTO, ON, OFF
	}
	
	private CameraHelper(){
		/*baseApi=new TessBaseAPI();
	    datapath = Environment.getExternalStorageDirectory() + "/lihh/";
	    File dir = new File(datapath+"tessdata/");
	    if (!dir.exists()){
	    	dir.mkdirs();
	    }
	    if(dir.exists()){
		    baseApi.init(datapath, language);
	    }*/
	}
	
	public static synchronized CameraHelper getInstance(){
		if(helper == null){
			helper = new CameraHelper();
		}
		return helper;
	}
	
	/**
	 * 设置照片质量
	 * @param picQuality
	 * @return
	 */
	public CameraHelper setPicQuality(int picQuality){
		this.picQuality = picQuality;
		return helper;
	}
	
	/**
	 * 设置闪光灯模式
	 * @param status
	 * @return
	 */
	public CameraHelper setFlashlight(Flashlight status){
		switch (status) {
		case AUTO:
			this.flashlightStatus = Camera.Parameters.FLASH_MODE_AUTO;
			break;
		case ON:
			this.flashlightStatus = Camera.Parameters.FLASH_MODE_ON;
			break;
		case OFF:
			this.flashlightStatus = Camera.Parameters.FLASH_MODE_OFF;
			break;
		default:
			this.flashlightStatus = Camera.Parameters.FLASH_MODE_AUTO;
		}
		return helper;
	}
	
	/**
	 * 设置文件保存路径(default: /mnt/sdcard/DICM)
	 * @param path
	 * @return
	 */
	public CameraHelper setPictureSaveDictionaryPath(String path){
		this.filePath = path;
		return helper;
	}
	
	public CameraHelper setMaskSurfaceView(MaskSurfaceView surfaceView){
		this.surfaceView = surfaceView;
		return helper;
	}
	
	/**
	 * 打开相机并开启预览
	 * @param holder		SurfaceHolder
	 * @param format		图片格式
	 * @param width			SurfaceView宽度
	 * @param height		SurfaceView高度
	 * @param screenWidth	屏幕宽度
	 * @param screenHeight	屏幕高度
	 */
	public void openCamera(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight, OnCaptureCallback callback){
		if(this.camera != null){
			this.camera.release();
		}
		if(st == null){
			st = new SleepTask();
		}
		this.camera = Camera.open();
		this.initParameters(holder, format, width, height, screenWidth, screenHeight);
		this.startPreview();
		ca = callback;
	}
	
	/**
	 * 开始自动扫描
	 */
	public void startFocus(){
		if(st!=null){
			st.cancel(true);
		}
		st = new SleepTask();
		st.execute(stId++);
	}
	
	/**
	 * 取消拍照
	 */
	public void cancelScan(){
		st.cancel(true);
		camera.cancelAutoFocus();
	}
	
	/**
	 * 照相
	 */
	public void tackPicture(){
		this.camera.autoFocus(new AutoFocusCallback() {
			@Override
			public void onAutoFocus(final boolean flag, final Camera camera) {
				long nowTime = new Date().getTime();
				if(decoding || (nowTime-firstTime) < CAMERA_STATE_DELAY){
					startFocus();
					return;
				}
				camera.takePicture(new ShutterCallback() {
					@Override
					public void onShutter() {
						// TODO 拍照完成处理，通常用于声音提醒等
					}
				}, null, new PictureCallback() {
					@Override
					public void onPictureTaken(final byte[] data, final Camera camera) {
						decoding = true;
						boolean success = false;
						// TODO 拍照处理
						final String filePath = savePicture(data);
						final String invoiceInfo = "251011562004#15462324#69192421";
						if(filePath != null){
//							baseApi.setImage(cutImage(data));
//			            	String text1= baseApi.getUTF8Text();
							success = true;
						}
						if(success){
							st.cancel(true);
//							声音提示
							if (tone == null) {
								tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
							}
							tone.startTone(ToneGenerator.TONE_PROP_BEEP);
							//停止相机预览
							stopPreview();
							ca.onCapture(success, invoiceInfo);
						}else{
							decoding = false;
							startFocus();
						}
					}
				});
			}
		});
	}
	
	/**
	 * 裁剪并保存照片
	 * @param data
	 * @return
	 */
	private String savePicture(byte[] data){
		File imgFileDir = getImageDir();
		if (!imgFileDir.exists() && !imgFileDir.mkdirs()) {
			return null;
		}
//		文件路径路径
		String imgFilePath = imgFileDir.getPath() + File.separator + this.generateFileName();;
		Bitmap b = this.cutImage(data);
		File imgFile = new File(imgFilePath);
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(imgFile);
			bos = new BufferedOutputStream(fos);
			b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		} catch (Exception error) {
			return null;
		} finally {
			try {
				if(fos != null){
					fos.flush();
					fos.close();
				}
				if(bos != null){
					bos.flush();
					bos.close();
				}
			} catch (IOException e) {}
		}
		return imgFilePath;
	}
	
	/**
	 * 生成图片名称
	 * @return
	 */
	private String generateFileName(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault());
		String strDate = dateFormat.format(new Date());
		return "img_" + strDate + ".jpg";
	}

	/**
	 * 
	 * @return
	 */
	private File getImageDir() {
		String path = null;
		if(this.filePath==null || this.filePath.equals("")){
			path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
		}else{
			path = Environment.getExternalStorageDirectory().getPath() + filePath;
		}
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
		return file;
	}
	
	/**
	 * 初始化相机参数
	 * @param holder		SurfaceHolder
	 * @param format		图片格式
	 * @param width			SurfaceView宽度
	 * @param height		SurfaceView高度
	 * @param screenWidth	屏幕宽度
	 * @param screenHeight	屏幕高度
	 */
	@SuppressWarnings("deprecation")
	private void initParameters(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight){
		try {
			Parameters p = this.camera.getParameters();
			
			this.camera.setPreviewDisplay(holder);
			
			if(width > height){
//				横屏
				this.camera.setDisplayOrientation(0);
			}else{
//				竖屏
				this.camera.setDisplayOrientation(90);
			}
			
//			照片质量
			p.set("jpeg-quality", picQuality);
			
//			设置照片格式
			p.setPictureFormat(PixelFormat.JPEG);
			
//			设置闪光灯
			p.setFlashMode(this.flashlightStatus);
			
			p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			
//			设置最佳预览尺寸
			List<Size> previewSizes = p.getSupportedPreviewSizes();
//			设置预览分辨率
			if(this.resolution == null){
				this.resolution = this.getOptimalPreviewSize(previewSizes, width, height);
			}
			try {
				p.setPreviewSize(this.resolution.width, this.resolution.height);
			} catch (Exception e) {
				Log.e(TAG, "不支持的相机预览分辨率: "+this.resolution.width+" × "+this.resolution.height);
			}

//			设置照片尺寸
			if(this.pictureSize == null){
				List<Size> pictureSizes = p.getSupportedPictureSizes();
				this.setPicutreSize(pictureSizes, screenWidth, screenHeight);
			}
			try {
				p.setPictureSize(this.pictureSize.width, this.pictureSize.height);
			} catch (Exception e) {
				Log.e(TAG, "不支持的照片尺寸: "+this.pictureSize.width+" × "+this.pictureSize.height);
			}
			
			this.camera.setParameters(p);
		} catch (Exception e) {
			Log.e(TAG, "相机参数设置错误");
		}
	}
	
	/**
	 * 释放Camera
	 */
	@SuppressWarnings("deprecation")
	public void releaseCamera(){
		if(this.camera!=null){
			if(this.isPreviewing){
				this.stopPreview(); 
			}
			this.camera.setPreviewCallback(null);
			isPreviewing = false;
			this.camera.release();
			this.camera = null;
		}
	}
	
	/**
	 * 停止预览
	 */
	@SuppressWarnings("deprecation")
	private void stopPreview(){
		if(this.camera!=null && this.isPreviewing){
			this.camera.stopPreview();
			this.isPreviewing = false;
		}
	}
	
	/**
	 * 开始预览
	 */
	@SuppressWarnings("deprecation")
	public void startPreview(){
		if(this.camera!=null){
			this.camera.startPreview();
			this.camera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(final boolean flag, final Camera camera) {

					//初始化拍照测试识别参数
					firstTime = new Date().getTime();
					decoding = false;
					startFocus();
				}
			});
			this.isPreviewing = true;
		}
	}
	
	
	/**
	 * 根据拍照byte[]数据生成Bitmap
	 * @param data
	 * @return
	 */
	private Bitmap getBitMapWithData(byte[] data){
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		if(this.surfaceView.getWidth() < this.surfaceView.getHeight()){
//			竖屏旋转照片
			Matrix matrix = new Matrix();
			matrix.reset();
			matrix.setRotate(90);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}
	
	/**
	 * 裁剪照片
	 * @param data
	 * @return
	 */
	private Bitmap cutImage(byte[] data){
		Bitmap bitmap = getBitMapWithData(data);
		
		if(this.surfaceView == null){
			return bitmap;
		}else{
			int[] sizes = this.surfaceView.getMaskSize();
			if(sizes[0]==0 || sizes[1]==0){
				return bitmap;
			}
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			int x = (w-sizes[0])/2;
			int y = (h-sizes[1])/2;
			return Bitmap.createBitmap(bitmap, x, y, sizes[0], sizes[1]);
		}
	}
	
	/**
	 * 剪辑并获取发票代码
	 * @return 发票代码
	 */
	int invoiceCode = 0;
	private void getInvoiceCode(Bitmap bitmap){
		int result = 0;
		
		invoiceCode = result;
	}
	/**
	 * 剪辑并获取发票号码
	 * @return 发票号码
	 */
	int invoiceNumber = 0;
	private void getInvoiceNumber(Bitmap bitmap){
		int result = 0;
		invoiceNumber = result;
	}
	/**
	 * 剪辑并获取发票兑奖密码
	 * @return  兑奖密码
	 */
	int invoicePassword = 0;
	private void getInvoicePassword(Bitmap bitmap){
		int result = 0;
		invoicePassword = result;
	}
	/**
	 * 兑奖
	 */
	private Boolean invoiceCash(){
		if(invoiceCode!=0 && invoiceNumber!=0 && invoicePassword!=0){
			return true;
		}
		return false;
	}

	/**
	 * 获取最佳预览尺寸
	 */
	@SuppressWarnings("deprecation")
	private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) width / height;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = height;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double r = size.width*1.0/size.height*1.0;
			if(r!=4/3 || r!=3/4 || r!=16/9 || r!=9/16){
				continue;
			}
			
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
	
	/**
	 * 设置照片尺寸为最接近屏幕尺寸
	 * @param list
	 * @return 
	 */
	private void setPicutreSize(List<Size> list, int screenWidth, int screenHeight){
		int approach = Integer.MAX_VALUE;
		
		for(Size size : list){
			int temp = Math.abs(size.width-screenWidth + size.height-screenHeight);
			System.out.println("approach: "+approach +", temp: "+ temp+", size.width: "+size.width+", size.height: "+size.height);
			if(approach > temp){
				approach = temp;
				this.pictureSize = size;
			}
		}
//		//降序
//		if(list.get(0).width>list.get(list.size()-1).width){
//			int len = list.size();
//			list = list.subList(0, len/2==0? len/2 : (len+1)/2);
//			this.pictureSize = list.get(list.size()-1);
//		}else{
//			int len = list.size();
//			list = list.subList(len/2==0? len/2 : (len-1)/2, len-1);
//			this.pictureSize = list.get(0);
//		}
	}
	
	class SleepTask extends AsyncTask<Integer, Integer, String>{

		@Override
		protected String doInBackground(Integer... params) {
			try {
				Thread.sleep(AUTO_FOCUS_DELAY);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tackPicture();
			return null;
		}
		
	}
}
