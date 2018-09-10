package com.wyman.playerlibrary.nicevideoplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by wyman
 * on 2018-09-07.
 */

public class NiceUtil {

    public static void showActionBar(Context context){
        if(context instanceof AppCompatActivity){
            ActionBar actionBar = ((AppCompatActivity)context).getSupportActionBar();
            if(actionBar != null){
                actionBar.setShowHideAnimationEnabled(false);
                actionBar.show();
            }
            ((AppCompatActivity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void hideActionBar(Context context){
        if(context instanceof AppCompatActivity){
            ActionBar actionBar = ((AppCompatActivity)context).getSupportActionBar();
            if(actionBar != null){
                actionBar.setShowHideAnimationEnabled(false);
                actionBar.hide();
            }
            ((AppCompatActivity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 获取屏幕宽度
     *
     * @param context
     * @return width of the screen.
     */
    public static int getScreenWidth(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @param context
     * @return heiht of the screen.
     */
    public static int getScreenHeight(Context context){
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * dp转px
     *
     * @param context
     * @param dpVal   dp value
     * @return px value
     */
    public static int dp2px(Context context,float dpVal){
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpVal,dm);
        int pxInt = (int)(px + 0.5);
        return pxInt;
    }

    /**
     * 将毫秒数格式化为"##:##"的时间
     *
     * @param milliseconds 毫秒数
     * @return ##:##
     */
    public static String formatTime(long milliseconds){
        if (milliseconds <= 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        long totalSeconds = milliseconds / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * 保存播放位置，以便下次播放时接着上次的位置继续播放.
     *
     * @param context
     * @param url     视频链接url
     */
    public static void savePlayPosition(Context context,String url,long position){
        SharedPreferences sp = context.getSharedPreferences("NICE_VIDEO_PALYER_PLAY_POSITION",Context.MODE_PRIVATE);
        sp.edit().putLong(url,position).apply();
    }

    /**
     * 取出上次保存的播放位置
     *
     * @param context
     * @param url     视频链接url
     * @return 上次保存的播放位置
     */
    public static long getSavePlayPosition(Context context,String url){
        SharedPreferences sp = context.getSharedPreferences("NICE_VIDEO_PALYER_PLAY_POSITION",Context.MODE_PRIVATE);
        return sp.getLong(url,0);
    }
}






















