package com.example.laomao.opencvdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CalculateSizeActivity extends BaseActivity {

    @BindView(R.id.apple_size)
    TextView mAppleSize;
    @BindView(R.id.apple_result)
    TextView mAppleResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_size);
        ButterKnife.bind(this);

        double area = getIntent().getDoubleExtra("area", 0);
        double sqrt = Math.sqrt(area / 3.14159);
        sqrt*=2;
        mAppleSize.setText("大小结果：" + (long)sqrt + "mm");

        String result;
        if (sqrt > 82) {
            result = "优等果";
        } else if (sqrt > 75) {
            result = "一等果";
        } else if (sqrt > 65) {
            result = "二等果";
        } else {
            result = "等外果";
        }

        mAppleResult.setText("该果为：" + result);
    }
}
