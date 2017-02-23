package me.rayyeung.permissionlibrary;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baishan.permissionlibrary.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.DialogInterface.OnCancelListener;
import static android.content.DialogInterface.OnClickListener;
import static android.support.v7.app.AlertDialog.Builder;

public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";
    public static final String SP_NAME = "permission";
    private static final String SP_KEY = "isFirst";

    public static final int PERMISSION_SINGLE = 1;

    public static final int CODE_SINGLE = 1;
    public static final int CODE_MUTI = 2;
    public static final int CODE_MUTI_SINGLE = 3;

    //未同意的权限
    private List<String> permissions = new ArrayList<>();
    private AlertDialog dialog;
    private android.support.v7.app.AlertDialog alertDialog;
    //权限记录
    private int index;
    private long start, end;
    //请求标示
    private int flag = 1;
    //app名称
    private CharSequence label;
    //申请权限类型
    private int type;
    //单个权限
    private String name;

    private PermissionUtils permissionUtils;
    private SharedPreferences sp;
    private boolean first;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getIntent().getIntExtra("type", 0);
        if (type == PERMISSION_SINGLE) {
            name = getIntent().getStringExtra("name");
            ActivityCompat.requestPermissions(PermissionActivity.this, new String[]{name}, CODE_SINGLE);
        } else {
            //当跳转到应用权限设置界面取消了之前的授权后，app会重启进程，根据判断可把当前界面直接finish
            sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
            first = sp.getBoolean(SP_KEY, true);
            if (!first) {
                updateSp(true);
                finish();
                return;
            }
            updateSp(false);
            permissionUtils = PermissionUtils.getInstance();
            label = getApplicationInfo().loadLabel(getPackageManager());
            checkPermission();
            if (permissions.size() > 0) {
                showPermissionDialog();
            }
        }

    }

    private void updateSp(boolean b) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(SP_KEY, b);
        editor.commit();
    }


    /**
     * 获取没有通过的权限
     */
    private void checkPermission() {
        for (String p : permissionUtils.getPermissions()) {
            int checkPermission = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(p);
            }
        }
    }

    private void showPermissionDialog() {
        View content = View.inflate(this, R.layout.request_permission_dialog, null);
        LinearLayout container = (LinearLayout) content.findViewById(R.id.permission_container);
        TextView tvTitle = (TextView) content.findViewById(R.id.tvTitle);
        TextView tvDesc = (TextView) content.findViewById(R.id.tvDesc);
        Button next = (Button) content.findViewById(R.id.goto_settings);
        tvTitle.setText(getString(R.string.dialog_title, label));
        tvDesc.setText(getString(R.string.dialog_msg, label));
        if (permissionUtils.getBtnBackgroundResource() > 0) {
            next.setBackgroundResource(permissionUtils.getBtnBackgroundResource());
        }
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String[] strs = new String[permissions.size()];
                ActivityCompat.requestPermissions(PermissionActivity.this, permissions.toArray(strs), CODE_MUTI);
            }
        });
        for (int i = 0; i < permissions.size(); i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.permission_info_item, container, false);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            TextView name = (TextView) view.findViewById(R.id.name);
            icon.setImageResource(permissionUtils.getImage(permissions.get(i)));
            name.setText(permissionUtils.getDescription(permissions.get(i)));
            container.addView(view);
        }
        dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                permissionUtils.close();
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CODE_SINGLE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionUtils.onGuarantee();
                } else {
                    permissionUtils.onDeny();
                }
                finish();
                break;
            case CODE_MUTI:
                this.permissions.clear();
                for (int i = 0; i < grantResults.length; i++) {
                    //可能是禁止 也可能是永久禁止
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        this.permissions.add(permissions[i]);
                    }
                }
                if (this.permissions.size() > 0) {
                    reRequestPermission(this.permissions.get(index));
                } else {
                    permissionUtils.finish();
                    finish();
                }
                break;
            case CODE_MUTI_SINGLE:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    end = System.currentTimeMillis();
                    //以时间判断作为用户前面点击了禁止询问后系统直接返回拒绝的情况
                    if (end - start < 100) {
                        showReason(permissions[index]);
                    } else {
                        reRequestPermission(permissions[index]);
                    }
                } else {
                    if (index < this.permissions.size() - 1) {
                        reRequestPermission(this.permissions.get(++index));
                    } else {
                        permissionUtils.finish();
                        finish();
                    }
                }
                break;
        }
    }


    /**
     * 无法获取权限 弹出提示
     *
     * @param permission
     */
    private void showReason(String permission) {
        String name = getPermissionName(permission);
        alertDialog = new Builder(this)
                .setTitle(String.format(getString(R.string.permission_title), name))
                .setMessage(String.format(getString(R.string.permission_denied_with_naac), label, name, label))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.reject), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                        permissionUtils.close();
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.go_to_setting), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flag = 3;
                        Uri packageURI = Uri.parse("package:" + getPackageName());
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                        startActivity(intent);
                    }
                }).show();
    }


    /**
     * 获取权限的说明名称
     *
     * @param permission
     * @return
     */
    public String getPermissionName(String permission) {
        String name = "";
        for (int i = 0; i < permissionUtils.getPermissions().length; i++) {
            if (permissionUtils.getPermissions()[i].equals(permission)) {
                name = permissionUtils.getDescriptions()[i];
                break;
            }
        }
        return name;
    }


    /**
     * 重新申请权限
     *
     * @param permission
     */
    private void reRequestPermission(final String permission) {
        String name = getPermissionName(permission);
        alertDialog = new Builder(this)
                .setTitle(String.format(getString(R.string.permission_title), name))
                .setMessage(String.format(getString(R.string.permission_denied), name, label))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                        permissionUtils.close();
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.ensure), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                        start = System.currentTimeMillis();
                        flag = 2;
                        ActivityCompat.requestPermissions(PermissionActivity.this, new String[]{permission}, CODE_MUTI_SINGLE);
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (flag == 3) {//去设置界面回来后  重新检查权限
            permissions.clear();
            checkPermission();
            if (permissions.size() > 0) {
                index = 0;
                reRequestPermission(permissions.get(index));
            } else {
                permissionUtils.finish();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //这里加判断是因为finish()方法执行之后没有立即调用到这里 甚至是界面重新启动之后onCreate再次执行了  这里才执行
        //这里是个坑 导致SP_KEY判断不准
        if (first) {
            updateSp(true);
        }
    }

    @Override
    public void onBackPressed() {
        permissionUtils.close();
        super.onBackPressed();
    }
}
