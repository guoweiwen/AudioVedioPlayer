package com.wyman.videoaudioplayer;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by wyman
 * on 2018-08-19.
 *
 * 也可以通过此方法 dp 与 px 转换
 * 下面两个方法都是取像素
 * TypedValue.complexToDimensionPixelOffset();
 * 取整数数去除小数点；
 * TypedValue.complexToDimensionPixelSize();
 * 将得出的数四舍五入；
 * 两个方法的区别就是 complexToDimensionPixelOffset 取整数
 *                complexToDimensionPixelSize 是4舍5入
 *  后面都是调用 TypedValue.applyDimension();
 *
 *  //density 是 屏幕密度 / 160
 *  float density = getResources().getDisplayMetrics().density;
 *  //densityDpi 是屏幕密度 如 160,320等
 *  float densityDpi = getResources().getDisplayMetrics().densityDpi;
 * */
public class DeviceUtils {

    /**
     * dip To px
     * */
    public static int dip2Px(float dipValue){
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        //每个设备的密度 scale = px / dp
        int px = (int)(dipValue * scale + 0.5);//加0.5为了4舍5入
        return px;
    }

    /**
     * px To dip
     * */
    public static int px2Dip(float pxValue){
        final float scale =  Resources.getSystem().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5);
    }

    /**
     * 获取屏幕高度dp为单位
     * DisplayMetrics类的density与densityDpi的区别：density 是DENSITY_DEFAULT：160
     * densityDpi 是DENSITY_LOW、DENSITY_MEDIUM、DENSITY_HIGH 120,160,240其中一个
     * */
    public static int getScreenWidthDp(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = (int)(dm.widthPixels / dm.density);
        return width;
    }

    /**
     * 获取屏幕高度dp
     * */
    public static int getScreenHeightDp(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int heightDp = (int)(dm.heightPixels / dm.density);
        return heightDp;
    }

    /**
     * 获取屏幕高度px
     * */
    public static int getScreenHeightPx(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    /**
     * 获取屏幕宽度px
     * */
    public static int getScreenWidthPx(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获取屏幕分辨率
     * */
    public static int[] getScreenResolution(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int[] screenResolutioin = new int[2];
        screenResolutioin[0] = dm.widthPixels;
        screenResolutioin[1] = dm.heightPixels;
        return screenResolutioin;
    }

    /**
     * 获取系统版本
     * */
    public static int getSystemVersion(){
        return Build.VERSION.SDK_INT;
    }

}
