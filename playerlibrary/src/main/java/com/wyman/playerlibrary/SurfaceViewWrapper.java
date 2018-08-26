package com.wyman.playerlibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * Created by wyman
 * on 2018-08-26.
 */

public class SurfaceViewWrapper extends SurfaceView {

    public SurfaceViewWrapper(Context context) {
        this(context,null);
    }

    public SurfaceViewWrapper(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SurfaceViewWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 测量宽高主要看 resolveSize 或 resolveSizeAndState 两个方法可以确定 MATCH_PARENT 时候和精确dp时候的值
     * WRAP_CONTENT 时候取什么值，需要自己填写
     * */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //最终宽高
        int widthSize,heightSize;
        int w,h;
        //宽度取 MATCH_PARENT
        w = MeasureSpec.getSize(widthMeasureSpec);
        //高度主要是 WRAP_CONTENT 时候取什么值
        h = dipTopx(200);//当 WRAP_CONTENT 时候取200高度
        widthSize = resolveSize(w,widthMeasureSpec);
        heightSize = resolveSize(h,heightMeasureSpec);
        setMeasuredDimension(widthSize,heightSize);
    }

    private int dipTopx(int dp) {
        int px;
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        px = (int)(dp * dm.density + 0.5);
        return px;
    }


}
