package com.wyman.videoaudioplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.wyman.videoaudioplayer.R;
import com.wyman.videoaudioplayer.utils.DeviceUtils;

import io.reactivex.functions.Consumer;

/**
 * app入口activity
 * */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button main_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main_button = findViewById(R.id.main_button);
        main_button.setOnClickListener(this);
        //系统大于等于 6.0 获取权限
        if(DeviceUtils.getSystemVersion() >= Build.VERSION_CODES.M){
            initRxpermission();
        }
    }

    /**
     * 系统6.0通过 Rxpermission 申请权限
     * */
    private void initRxpermission() {
        //创建了一个 RxPermissionsFragment 通过Fragment 的 requestPermissions
        // onRequestPermissionsResult 而避免用 Activity 的
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.requestEach(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA//,
               /* Manifest.permission.READ_CALENDAR,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS*/)
                /**
                 * 通过 RxPermissionsFragment 遍历permissions数组 subject.onNext() 让订阅者即这里
                 * subscribe 处理
                 * */
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if(permission.granted){
                            // 用户已经同意该权限
                            Log.d(TAG, permission.name + " is granted.");
                        } else if(permission.shouldShowRequestPermissionRationale){
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            Log.d(TAG, permission.name + " is denied. More info should be provided.");
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.d(TAG, permission.name + " is denied.");
                        }
                    }
                });
    }

    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_PORTRAIT){//竖屏
            mediaPlayerWrapperView.fullScreen = false;
        } else {
            mediaPlayerWrapperView.fullScreen = true;
        }
    }
    */

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.main_button){
            startActivity(new Intent(this, VideoRecordActivity.class));
        }
    }
}
