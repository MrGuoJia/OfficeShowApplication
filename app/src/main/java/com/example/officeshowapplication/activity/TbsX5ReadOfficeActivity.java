package com.example.officeshowapplication.activity;

import android.os.Bundle;
import android.os.Environment;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.officeshowapplication.R;
import com.tencent.smtt.sdk.TbsReaderView;

import java.io.File;

public class TbsX5ReadOfficeActivity extends AppCompatActivity implements TbsReaderView.ReaderCallback {
    RelativeLayout rootRl;
    private TbsReaderView tbsReaderView;
    private String fileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tbs_x5_read_office);
        fileUrl = getIntent().getStringExtra("fileUrl");

        initTbs();
    }

    private void initTbs() {
        rootRl = findViewById(R.id.rootRl);
        tbsReaderView = new TbsReaderView(this, this);
        rootRl.addView(tbsReaderView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        Bundle bundle = new Bundle();
        bundle.putString("filePath", fileUrl);
        //加载插件保存的路径
        bundle.putString("tempPath", Environment.getExternalStorageDirectory() + File.separator + "temp");
        boolean b = tbsReaderView.preOpen("docx", false);
        if (b) {
            tbsReaderView.openFile(bundle);
        }

    }

    @Override
    public void onCallBackAction(Integer integer, Object o, Object o1) {

    }


}
