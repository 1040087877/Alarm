package com.ruanjianerban.linyangbin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ruanjianerban.alarm.R;

/**
 * Created by 10400 on 2016/9/21.
 */
public class settingAlarmActivity extends Activity{

    private TimePicker timePicker;
    private int h;
    private int m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.setting_alarm);
        setView();
    }

    private void setView() {
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        Button cancle = (Button) findViewById(R.id.cancle);
        Button sure = (Button) findViewById(R.id.sure);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle!=null){
            String time = bundle.getString("time");
            String[] split = time.split(":");
            int strH =Integer.parseInt(split[0]);
            int strM =Integer.parseInt(split[1]);
            timePicker.setCurrentHour(strH);
            timePicker.setCurrentMinute(strM);
        }

        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(h==0 && settingAlarmActivity.this.m ==0){
                    Toast.makeText(getApplicationContext(),"你还没设置时间，请再试一遍",Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt("h",h);
                bundle.putInt("m", settingAlarmActivity.this.m);
                setResult(11,intent.putExtras(bundle));
                finish();
            }
        });

        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
//                Log.e("TAG", "onTimeChanged: "+hourOfDay+":"+minute);
                h = hourOfDay;
                settingAlarmActivity.this.m = minute;
            }
        });

    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.e("TAG", "requestCode: "+requestCode+"\nresultCode:  "+resultCode);
//    }
}
