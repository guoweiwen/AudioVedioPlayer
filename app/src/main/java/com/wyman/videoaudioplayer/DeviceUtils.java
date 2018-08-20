package com.wyman.videoaudioplayer;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by wyman
 * on 2018-08-19.
 */
public class DeviceUtils {

    /**
     * dip To px
     * */
    public static int dip2Px(float dipValue){
        final float scale = Resources.getSystem().getDisplayMetrics().densityDpi;
        //每个设备的密度 scale = px / dp
        double px = dipValue * scale + 0.5;//加0.5为了4舍5入
        return Integer.getInteger(String.valueOf(px));
    }

    /**
     * px To dip
     * */
    public static int px2Dip(float pxValue){
        final int scale =  Resources.getSystem().getDisplayMetrics().densityDpi;
        return Integer.getInteger(String.valueOf(pxValue / scale + 0.5));
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
        int width = Integer.parseInt(String.valueOf(dm.widthPixels / dm.density));
        return width;
    }

    /**
     * 获取屏幕高度dp
     * */
    public static int getScreenHeightDp(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int heightDp = Integer.parseInt(String.valueOf(dm.heightPixels / dm.density));
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
}
