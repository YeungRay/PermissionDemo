package me.rayyeung.permissiondemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.rayyeung.permissionlibrary.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * demo中只有一个antivity 所以直接写在这里 实际项目中建议写在BaseActivity中
         */
        if (PermissionUtils.getInstance().check(this)) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        }
    }
}
