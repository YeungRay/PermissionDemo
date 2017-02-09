package me.rayyeung.permissionlibrary;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;

import com.baishan.permissionlibrary.R;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 权限管理工具
 * 注意！！！一定要AndroidManifest.xml中申请权限
 *
 * Created by RayYeung on 2016/9/9.
 */
public class PermissionUtils {

    //默认3种常用权限
    private String[] permissions = {WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, CAMERA};
    private int[] images = {R.drawable.permission_ic_memory, R.drawable.permission_ic_location, R.drawable.permission_ic_camera};
    private String[] descriptions = {"存储空间", "位置信息", "拍照权限"};

    private static PermissionUtils instance;
    private Callback permissionCallback;
    private SingleCallback perPermissionCallback;

    private boolean isOpen;

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
     *
     * @param images
     */
    public void setImages(int[] images) {
        if (images != null && images.length == permissions.length) {
            this.images = images;
        }
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

    /***
     * 设置权限描述
     *
     * @param descriptions
     */
    public void setDescriptions(String[] descriptions) {
        if (descriptions != null && descriptions.length == permissions.length) {
            this.descriptions = descriptions;
        }
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
        for (String p : permissions) {
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


    public void openActivity() {
        isOpen = true;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void onDeny() {
        perPermissionCallback.onDeny();
    }

    public void onGuarantee() {
        perPermissionCallback.onGuarantee();
    }

    public void close() {
        isOpen = false;
        permissionCallback.onClose();
    }

    public void finish() {
        isOpen = false;
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
