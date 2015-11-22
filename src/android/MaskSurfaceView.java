package com.phonegap.bossbolo.plugin.mediacapture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;


@SuppressWarnings("deprecation")
public class MaskSurfaceView extends FrameLayout{

	//手机屏幕密度
	private static float density;
	//提示信息字体大小
	private static final int TEXT_SIZE = 16;
	//文字与透明框顶部间距
	private static final int TEXT_PADDING_TOP = 30;
	
	//取消按钮与透明框顶部间距
	private static final int CANCEL_PADDING_BOTTOM = 30;
	
	//取景框自刷新时间
	private static final long ANIMATION_DELAY = 15L;
	//扫描线每次刷新移动的距离 
    private static final int SPEEN_DISTANCE = 5;
    //扫描线位置
    private int slideY;
    //是否首次布局
    private Boolean isfirst = true;
    //扫描线的与扫描框左右的间隙
    private static final int MIDDLE_LINE_PADDING = 5;
    //扫描线线宽
    private static final int MIDDLE_LINE_WIDTH = 5;
    //四个绿色边角对应的宽度
    private static final int CORNER_WIDTH = 4;
    //四个绿色边角对应的长度 
    private int CORNER_LENGTH = 30;
    
    private OnCaptureCallback ca;
    
    private int width_padding = 300;
	
	private MSurfaceView surfaceView;
	private MaskView imageView;
	private int width;
	private int height;
	private int maskWidth;
	private int maskHeight;
	private int screenWidth;
	private int screenHeight;
	
	private FakeR fakeR;

	public MaskSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.fakeR = FakeR.getInstance();
		surfaceView = new MSurfaceView(context);
		imageView = new MaskView(context);
		this.addView(surfaceView,LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addView(imageView,LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		screenHeight = display.getHeight();
		screenWidth = display.getWidth();
		density = context.getResources().getDisplayMetrics().density; 
		CameraHelper.getInstance().setMaskSurfaceView(this);
	}
	
	public void initMask(Integer width, Integer height, OnCaptureCallback callback){
		maskHeight = height;
		maskWidth = width;
		ca = callback;
	}
	
	public void setMaskSize(Integer width, Integer height){
		maskHeight = height;
		maskWidth = width;
	}
	
	public int[] getMaskSize(){
		return new MaskSize().size;
	}
	
	private class MSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
		private SurfaceHolder holder;
		public MSurfaceView(Context context) {
			super(context);
			this.holder = this.getHolder();
			//translucent半透明 transparent透明
			this.holder.setFormat(PixelFormat.TRANSPARENT);
			this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			this.holder.addCallback(this);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			width = w;
			height = h;
//			CameraHelper.getInstance().startFocus(RectCameraActivity.this);
			CameraHelper.getInstance().openCamera(holder, format, width, height, screenWidth, screenHeight, ca);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			int i = 0;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			CameraHelper.getInstance().releaseCamera();
		}
	}

	private class MaskSize{
		private int[] size;
		private MaskSize(){
			this.size = new int[]{maskWidth, maskHeight, width, height};
		}
	}
	
	private class MaskView extends View{
		private Paint lightPaint;
		private Paint linePaint;
		private Paint rectPaint;
//		private Paint textPaint;
		public MaskView(Context context) {
			super(context);
			
			//透明区域边角线
			lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//			lightPaint.setColor(Color.BLUE);
			lightPaint.setStyle(Style.STROKE);
			lightPaint.setStrokeWidth(CORNER_WIDTH);
			lightPaint.setAlpha(50);
			
			//定义刷新线Paint
			linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			linePaint.setColor(Color.BLUE);
			linePaint.setStyle(Style.STROKE);
			linePaint.setStrokeWidth(MIDDLE_LINE_WIDTH);
			linePaint.setAlpha(50);
			
			//定义四周矩形阴影区域
			rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			rectPaint.setColor(Color.BLACK);
			rectPaint.setStyle(Style.FILL);
			rectPaint.setAlpha(50);
			
			//提示文字
			/*textPaint = rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			textPaint.setColor(Color.BLACK);
			textPaint.setStyle(Style.FILL);
			textPaint.setColor(Color.WHITE);  
			textPaint.setTextSize(TEXT_SIZE * density);
			textPaint.setTypeface(Typeface.create("System", Typeface.BOLD));*/ 
		}
		@Override
		protected void onDraw(Canvas canvas) {
			if(maskHeight==0 && maskWidth==0){
				return;
			}
			if(maskHeight==height || maskWidth==width){
				return;
			}

			//横竖屏判断
			Boolean isVertical = true;
			if((height>width&&maskHeight<maskWidth) || (height<width&&maskHeight>maskWidth)){
				int temp = maskHeight;
				maskHeight = maskWidth;
				maskWidth = temp;
				isVertical = false;
			}
			
			//根据屏幕实际高宽、需求高宽的比例以及默认对透明部分高宽进行重新计算
			if(isVertical){
				width_padding = width*3/5;
				int temp = maskWidth;
				maskWidth = width_padding;
				maskHeight = Math.abs(maskHeight*maskWidth/temp);
			}else{
				width_padding = height*3/5;
				int temp = maskHeight;
				maskHeight = width_padding;
				maskWidth = Math.abs(maskWidth*maskHeight/temp);
			}
			
			//计算四周边距
			int h = Math.abs((height-maskHeight)/2);
			int w = Math.abs((width-maskWidth)/2);
			
			int top = h;
			int bottom = h + maskHeight;
			int left = w;
			int right = w + maskWidth;


			if(isfirst){
				slideY = h;
				isfirst = false;
			}
			
			//画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面  
		    //扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边  
		    canvas.drawRect(0, 0, width, top, rectPaint);  //上
		    canvas.drawRect(0, top, left, bottom+1, rectPaint);  //左
		    canvas.drawRect(right + 1, top, width, bottom + 1,  rectPaint);  //右
		    canvas.drawRect(0, bottom + 1, width, height, rectPaint); //下
		    
		    //画扫描框边上的角，总共8个部分  
		    lightPaint.setColor(Color.GREEN);  
	        canvas.drawRect(left, top-CORNER_WIDTH, left + CORNER_LENGTH,  top, lightPaint);  
	        canvas.drawRect(left-CORNER_WIDTH, top-CORNER_WIDTH,  left,  top + CORNER_LENGTH, lightPaint);  
	        canvas.drawRect( right-CORNER_LENGTH,  top-CORNER_WIDTH,  right, top, lightPaint);  
	        canvas.drawRect( right, top-CORNER_WIDTH,  right+CORNER_WIDTH,  top + CORNER_LENGTH, lightPaint);
	        
	        canvas.drawRect( left, bottom,  left + CORNER_LENGTH,  bottom + CORNER_WIDTH, lightPaint);  
	        canvas.drawRect( left-CORNER_WIDTH, bottom - CORNER_LENGTH, left ,  bottom+CORNER_WIDTH, lightPaint);  
	        canvas.drawRect( right,  bottom - CORNER_LENGTH, right + CORNER_WIDTH,  bottom, lightPaint);  
	        canvas.drawRect( right - CORNER_LENGTH,  bottom, right + CORNER_WIDTH,  bottom + CORNER_WIDTH, lightPaint);
			
			//绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE  
			slideY += SPEEN_DISTANCE;  
			if(slideY >= height-h){
				slideY = h;  
			}  
			canvas.drawRect(w + MIDDLE_LINE_PADDING, slideY-MIDDLE_LINE_PADDING/2,
					w+maskWidth-MIDDLE_LINE_PADDING, slideY+MIDDLE_LINE_WIDTH/2, linePaint);
			//仅刷新取景框区域  
	        postInvalidateDelayed(ANIMATION_DELAY, w, h, w+maskWidth, h+maskHeight);
	        
			super.onDraw(canvas);
		}
	}
}
