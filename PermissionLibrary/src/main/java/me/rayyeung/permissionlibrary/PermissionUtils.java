package me.rayyeung.permissionlibrary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;

import com.baishan.permissionlibrary.R;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 权限管理工具
 * 注意！！！一定要在AndroidManifest.xml中申请权限
 * <p>
 * Created by RayYeung on 2016/9/9.
 */
public class PermissionUtils {

    private static final String COUNT = "count";
    private static final String KEY_PREFIX = "permission";
    private static final String PID = "pid";

    //默认3种常用权限
    private String[] permissions = {WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, CAMERA};
    private int[] images = {R.drawable.permission_ic_memory, R.drawable.permission_ic_location, R.drawable.permission_ic_camera};
    private String[] descriptions = {"存储空间", "位置信息", "拍照权限"};

    private static PermissionUtils instance;
    private Callback permissionCallback;
    private SingleCallback perPermissionCallback;


    private int btnBackgroundResource = 0;

    private PermissionUtils() {

    }

    public static PermissionUtils getInstance() {
        if (instance == null) {
            instance = new PermissionUtils();
        }
        return instance;
    }

    /**
     * 设置权限
     * 在 {@link PermissionUtils#checkMutiPermission}之前调用
     *
     * @param permissions
     */
    public void setPermissions(String[] permissions) {
        if (permissions != null && permissions.length > 0) {
            this.permissions = permissions;
        }
    }

    /**
     * 设置权限图片
     * 在 {@link PermissionUtils#checkMutiPermission}之前调用
     *
     * @param images
     */
    public void setImages(int[] images) {
        if (images != null && images.length == permissions.length) {
            this.images = images;
        }
    }


    /***
     * 设置权限描述
     * 在 {@link PermissionUtils#checkMutiPermission}之前调用
     *
     * @param descriptions
     */
    public void setDescriptions(String[] descriptions) {
        if (descriptions != null && descriptions.length == permissions.length) {
            this.descriptions = descriptions;
        }
    }


    /**
     * 设置单个权限
     * @param index
     * @param permission
     */
    public void setPermission(int index, String permission) {
        if (index < 0 || index > permissions.length - 1) {
            return;
        }
        permissions[index] = permission;
    }

    public void setImage(int index, int image) {
        if (index < 0 || index > images.length - 1) {
            return;
        }
        images[index] = image;
    }

    public void setDescription(int index, String description) {
        if (index < 0 || index > descriptions.length - 1) {
            return;
        }
        descriptions[index] = description;
    }

    /**
     * 设置按钮背景
     * 在 {@link PermissionUtils#checkMutiPermission}之前调用
     *
     * @param resid
     */
    public void setBtnBackgroundResource(@DrawableRes int resid) {
        this.btnBackgroundResource = resid;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public int[] getImages() {
        return images;
    }

    public String[] getDescriptions() {
        return descriptions;
    }

    public int getImage(String permission) {
        int image = 0;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(permission)) {
                image = images[i];
                break;
            }
        }
        return image;
    }

    public String getDescription(String permission) {
        String description = null;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(permission)) {
                description = descriptions[i];
                break;
            }
        }
        return description;
    }

    public int getBtnBackgroundResource() {
        return btnBackgroundResource;
    }

    /**
     * 检查是否需要权限申请(多个权限)
     *
     * @param context
     * @return false  不需要 true 需要
     */
    public boolean check(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        SharedPreferences sp = context.getSharedPreferences(PermissionActivity.SP_NAME, Context.MODE_PRIVATE);
        int pid = sp.getInt(PID, 0);
        if (Process.myPid() != pid) { //不在同一个进程
            int count = sp.getInt(COUNT, 0);
            if (count == 0) return false;
            SharedPreferences.Editor editor = sp.edit();
            permissions = new String[count];
            for (int i = 0; i < count; i++) {
                permissions[i] = sp.getString(KEY_PREFIX + i, "");
            }
        }
        for (String p : permissions) {
            System.out.println(p);
            if (!checkPermission(context, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查多个权限并申请  回调结果
     *
     * @param context
     * @param callback
     */
    public void checkMutiPermission(Context context, Callback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback.onFinish();
            return;
        }
        boolean flag = false;
        for (String p : permissions) {
            if (!checkPermission(context, p)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            //保存需要申请的权限
            SharedPreferences sp = context.getSharedPreferences(PermissionActivity.SP_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(PID, Process.myPid());
            editor.putInt(COUNT, permissions.length);
            for (int i = 0; i < permissions.length; i++) {
                editor.putString(KEY_PREFIX + i, permissions[i]);
            }
            editor.commit();
            permissionCallback = callback;
            context.startActivity(new Intent(context, PermissionActivity.class));
        } else {
            callback.onFinish();
        }
    }

    /**
     * 检查单个权限  回调处理
     *
     * @param context
     * @param permission
     * @param callback
     */
    public void checkSinglePermission(Context context, String permission, SingleCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback.onGuarantee();
            return;
        }
        if (checkPermission(context, permission)) {
            callback.onGuarantee();
        } else {
            perPermissionCallback = callback;
            Intent it = new Intent(context, PermissionActivity.class);
            it.putExtra("type", PermissionActivity.PERMISSION_SINGLE);
            it.putExtra("name", permission);
            context.startActivity(it);
        }
    }


    /**
     * 检查权限是否已经同意
     *
     * @param context
     * @param permission
     * @return
     */
    private static boolean checkPermission(Context context, String permission) {
        int checkPermission = ContextCompat.checkSelfPermission(context, permission);
        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }


    public void onDeny() {
        perPermissionCallback.onDeny();
    }

    public void onGuarantee() {
        perPermissionCallback.onGuarantee();
    }

    public void close() {
        if (permissionCallback != null)
            permissionCallback.onClose();
    }

    public void finish() {
        if (permissionCallback != null)
            permissionCallback.onFinish();
    }

    /**
     * 申请多个权限回调
     */
    public interface Callback {
        /**
         * 拒绝权限或者关闭申请框
         */
        void onClose();

        /**
         * 权限都通过 正常完成
         */
        void onFinish();
    }

    public interface SingleCallback {
        void onDeny();

        void onGuarantee();

    }


}
