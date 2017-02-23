package me.rayyeung.permissiondemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import me.rayyeung.permissionlibrary.PermissionUtils;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        PermissionUtils utils = PermissionUtils.getInstance();
        //utils.setBtnBackgroundResource(R.drawable.shape_btn_bg);
        //utils.setDescriptions(new String[]{"权限1","权限2","权限3"});
        utils.getDescriptions()[2] = "手机信息";
        utils.getPermissions()[2] = Manifest.permission.READ_PHONE_STATE;
        utils.checkMutiPermission(this, new PermissionUtils.Callback() {
            @Override
            public void onClose() {
                finish();
            }

            @Override
            public void onFinish() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                }, 2000);

            }
        });
    }


}
