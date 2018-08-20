package com.wyman.videoaudioplayer;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Created by wyman
 * on 2018-08-19.
 * 文件操作工具类
 */
public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * 在手机内存卡（非外置）创建文件
     * @param environment 根据Environment标准目录如：@see Environment.DIRECTORY_PICTURES
     *                                           @see Environment.DIRECTORY_MOVIES
     * @param suffixName 格式后缀名字 如：.mp4 .jpg
     * @return 返回以时间 yyyyMMddHHmmss为名称的File类
     * */
    public static File createExternalStorageFile(String environment,String suffixName){
        File file = null;
        if(Environment.getExternalStorageState() == Environment.MEDIA_UNMOUNTED)
            throw new IllegalStateException("ExternalStorage Unmounted...");
        Objects.requireNonNull(environment,"environment is empty");//Objects since jdk 1.7
        Objects.requireNonNull(environment,"suffixName is empty");
        if(suffixName.indexOf('.') == -1)
            throw new IllegalStateException("suffixName isn't contain \".\"");
        if(Environment.isExternalStorageEmulated()
                || Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED){
            if(environment.equals(Environment.DIRECTORY_MOVIES)){
                file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            } else if(environment.equals(Environment.DIRECTORY_PICTURES)){
                file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            } else if(environment.equals(Environment.DIRECTORY_MUSIC)){
                file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            }
            file = new File(file,"Wyman");
            if(!file.exists()){
                file.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String dataPaht = sdf.format(new Date());
            file = new File(file.getAbsoluteFile(),dataPaht + suffixName);
        }
        return file;
    }

    /**
     * 在手机内存卡（非外置）创建文件
     * @param environment 根据Environment标准目录如：@see Environment.DIRECTORY_PICTURES
     *                                           @see Environment.DIRECTORY_MOVIES
     * @param suffixName 格式后缀名字 如：.mp4 .jpg
     * @return 返回以时间 yyyyMMddHHmmss为名称的String路径
     * */
    public static String createExternalStorageFilePath(String environment,String suffixName){
        return createExternalStorageFile(environment,suffixName).toString();
    }

}
