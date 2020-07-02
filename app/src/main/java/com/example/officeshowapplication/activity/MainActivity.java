package com.example.officeshowapplication.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.officeshowapplication.R;
import com.example.officeshowapplication.utils.FileUtils;
import com.tencent.smtt.sdk.QbSdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_openWithWPS, btn_openWithTbs;
    private String fileUrl;//assets 文件夹下的资源路径
    final static int MY_PERMISSIONS_REQUEST_CALL_PHONE = 10086;

    private boolean ifInitSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //博客上有 解决FileUriExposedException 的方案之一
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }*/

        /**
         * 检查写入权限问题，需要申请权限则在申请后再将文件写入手机
         * */
        checkPermissions();
        initQBSDK();

        initView();

    }

    private void initQBSDK() {
        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                Log.e("QbSdk", "QbSdk onCoreInitFinished");
            }

            @Override
            public void onViewInitFinished(boolean b) {

                ifInitSuccess = b;
                Log.e("QbSdk", "QbSdk 初始化是否成功：" + b);
            }
        });
        QbSdk.setDownloadWithoutWifi(true);//设置支持非Wifi下载

    }

    private void checkPermissions() {
        //检查是否获得权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有获得授权，申请授权
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //弹窗解释为何需要该权限，再次请求权限
                Toast.makeText(MainActivity.this, "请授权，否则无法存储test.docx 文档", Toast.LENGTH_LONG).show();
                //跳转到应用设置界面
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } else {
                //不需要解释为何需要授权直接请求授权
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
            }
        } else {
            //获得授权，将文件写入到
            saveFileToPhone();

        }

    }


    /**
     * 将assets目录下的文件复制到手机指定位置
     */
    private void saveFileToPhone() {
        FileUtils.getInstance(getApplicationContext()).copyAssetsToSD("", "officeShow/office").setFileOperateCallback(new FileUtils.FileOperateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "保存成功，可以打开啦", Toast.LENGTH_SHORT).show();
                fileUrl = Environment.getExternalStorageDirectory() + "/officeShow/office/test.docx";
            }

            @Override
            public void onFailed(String error) {
                Toast.makeText(MainActivity.this, "保存失败：" + error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initView() {
        btn_openWithWPS = findViewById(R.id.btn_openWithWPS);
        btn_openWithWPS.setOnClickListener(this);

        btn_openWithTbs = findViewById(R.id.btn_openWithTbs);
        btn_openWithTbs.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_openWithWPS:
                if (!isInstall(this, "cn.wps.moffice_eng")) {
                    Toast.makeText(this, "请下载安装WPS", Toast.LENGTH_SHORT).show();
                    return;
                }

                startActivity(getWordFileIntent(fileUrl));
                break;


            case R.id.btn_openWithTbs:
                if(!ifInitSuccess){
                    Toast.makeText(this, "初始化失败，请查看原因", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(this,TbsX5ReadOfficeActivity.class)
                .putExtra("fileUrl",fileUrl)
                );
                break;
        }
    }


    private Intent getWordFileIntent(String Path) {
        File file = new File(Path);
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        //Uri uri = Uri.fromFile(file);//解决FileUriExposedException
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            uri = FileProvider.getUriForFile(getApplicationContext(),
                    getPackageName() + ".fileprovider", new File(String.valueOf(file)));
        } else {
            uri = Uri.fromFile(new File(String.valueOf(file)));
        }


        String type = getMIMEType(file);
        if (type.contains("pdf") || type.contains("vnd.ms-powerpoint") || type.contains("vnd.ms-word") || type.contains("vnd.ms-excel") || type.contains("text/plain") || type.contains("text/html")) {
            if (isInstall(this, "cn.wps.moffice_eng")) {
                intent.setClassName("cn.wps.moffice_eng",
                        "cn.wps.moffice.documentmanager.PreStartActivity2");
                intent.setData(uri);
            } else {
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setDataAndType(uri, type);
            }
        } else {
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setDataAndType(uri, type);
        }
        return intent;
    }


    /**
     * 判断文件类型
     */
    private static String getMIMEType(File f) {
        String type = "";
        String fName = f.getName();
        /* 取得扩展名 */
        String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        /* 依扩展名的类型决定MimeType */
        if (end.equals("pdf")) {
            type = "application/pdf";
        } else if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") ||
                end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
            type = "audio/*";
        } else if (end.equals("3gp") || end.equals("mp4")) {
            type = "video/*";
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png") ||
                end.equals("jpeg") || end.equals("bmp")) {
            type = "image/*";
        } else if (end.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else if (end.equals("pptx") || end.equals("ppt")) {
            type = "application/vnd.ms-powerpoint";
        } else if (end.equals("docx") || end.equals("doc")) {
            type = "application/vnd.ms-word";
        } else if (end.equals("xlsx") || end.equals("xls")) {
            type = "application/vnd.ms-excel";
        } else if (end.equals("txt")) {
            type = "text/plain";
        } else if (end.equals("html") || end.equals("htm")) {
            type = "text/html";
        } else {
            //如果无法直接打开，就跳出软件列表给用户选择
            type = "*/*";
        }
        return type;
    }


    /**
     * 判断是否有该程序的安装包
     */
    private boolean isInstall(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        // 获取所有已安装程序的包信息
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (pinfo.get(i).packageName.equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //授权成功，写入文件
                    saveFileToPhone();
                } else {
                    //授权失败
                    Toast.makeText(this, "授权失败！", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


}
