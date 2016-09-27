package com.ruanjianerban.linyangbin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruanjianerban.alarm.R;
import com.ruanjianerban.linyangbin.javabean.alarmClass;
import com.ruanjianerban.linyangbin.service.AlarmService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ListView lvTime;
    private TextView tvTime;
    private Switch swTime;
    private int pos;
    private List<alarmClass> alarmList;
    private MyBaseAdapter baseAdapter;
    private SharedPreferences.Editor edit;
    private SharedPreferences sp;
    private AlarmManager alarmManager;
    private PendingIntent pi;
    private Calendar cal;
    private Intent alarmIntent;

    public Handler mHandler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            tvTime.setText(getSysTime());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setView();
        refreshTime();
        setData();
    }

    private void setData() {
        sp = getSharedPreferences("date", this.MODE_APPEND);
        edit = sp.edit();
        alarmList = new ArrayList<>();
        alarmList = getDateList();
        baseAdapter = new MyBaseAdapter();
        if (alarmList != null) {
            lvTime.setAdapter(baseAdapter);
        }
        lvTime.setOnCreateContextMenuListener(this);
        alarmSetting();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(1, 11, 0, "删除");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int pos = info.position;
        if (item.getItemId() == 11) {
            alarmList.remove(pos);
            saveData();
            baseAdapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(), "删除成功", Toast.LENGTH_SHORT).show();
        }
        return super.onContextItemSelected(item);
    }

    private void setView() {
        lvTime = (ListView) findViewById(R.id.lv_time);
//        swTime = (Switch) findViewById(R.id.sw_time);
        tvTime = (TextView) findViewById(R.id.tv_time);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //添加事件 点击事件
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(getApplicationContext(),"你点击了",Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(new Intent(getApplicationContext(),
                        settingAlarmActivity.class)), 2);
            }
        });
        //修改事件，listview item点击事件
        lvTime.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.e("TAG", "setOnItemSelectedListener: "+position);
                startActivityForResult(new Intent(new Intent(getApplicationContext(),
                        settingAlarmActivity.class)).putExtra("time", alarmList.get(position).time), 1);
                pos = position;
            }
        });
        //暂时不处理长按事件
        lvTime.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getApplicationContext(),"长按了",Toast.LENGTH_SHORT).show();
//                new AlertDialog.Builder(getApplicationContext()).setItems();
                return false;
            }
        });
    }

    public String getSysTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        long time = new Date().getTime();
        String format = simpleDateFormat.format(time);
        return format;
    }

    public void refreshTime(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                Message message = new Message();
                mHandler.sendEmptyMessage(0);

            }
        },0,20*1000);
    }


    public List<alarmClass> getDateList() {
        Gson gson = new Gson();
        String strJson = sp.getString("timeJson", null);
        List<alarmClass> dateList = gson.fromJson(strJson, new TypeToken<List<alarmClass>>() {
        }.getType());
        if (dateList == null) {
            dateList = new ArrayList<>();
        }
        return dateList;
    }

    class MyBaseAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return alarmList.size();
        }

        @Override
        public Object getItem(int position) {
            return alarmList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(getApplicationContext(), R.layout.item_alarm, null);
            TextView itemTime = (TextView) convertView.findViewById(R.id.item_time);
            TextView timeStatus = (TextView) convertView.findViewById(R.id.time_status);
            Switch switchTime = (Switch) convertView.findViewById(R.id.sw_time);
            itemTime.setText(alarmList.get(position).time);
            switchTime.setChecked(alarmList.get(position).checkStatus);
            switchTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Log.e("TAG", "isChecked: " + isChecked+ "position: " + position);
                    alarmList.get(position).checkStatus = isChecked;
                    if (alarmList.get(position).checkStatus) {
                        alarmList.get(position).timeStatus = "已开启";
                        //获取时间并且开启服务
                        pi = PendingIntent.getService(getApplicationContext(), position, alarmIntent, 0);
                        alarmClass alarmtime = alarmList.get(position);
                        String[] split = alarmtime.time.split(":");
                        int h = Integer.parseInt(split[0]);
                        int m = Integer.parseInt(split[1]);
                        cal.set(Calendar.HOUR_OF_DAY, h);
                        cal.set(Calendar.MINUTE, m);

                        //测试--把data格式化 yyyy-MM--dd
                        SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        String format = sim.format(cal.getTime());
                        Log.e("TAG", "Calendar: " + format);

//                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),3*1000,pi);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    } else {
                        //取消闹钟
                        alarmList.get(position).timeStatus = "未开启";
                        PendingIntent piCancel= PendingIntent.getService(getApplicationContext(), position, alarmIntent, 0);
                        alarmManager.cancel(piCancel);
                    }
                    baseAdapter.notifyDataSetChanged();
                }
            });
            timeStatus.setText(alarmList.get(position).timeStatus);
//            swTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
////                    Log.e("TAG", "onCheckedChanged: " + isChecked + "position: " + position);
//                }
//            });
            return convertView;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.e("TAG", "requestCode: "+requestCode+"\nresultCode:  "+resultCode);
        if (requestCode == 1) {
            alarmClass alarmPos = alarmList.get(pos);
            if (data != null) {
                Bundle bundle = data.getExtras();
                if (!bundle.isEmpty()) {
                    int h = data.getExtras().getInt("h");
                    int m = data.getExtras().getInt("m");
//                    Log.e("TAG", "requestCode: "+requestCode+"\nresultCode:  "+resultCode+"\n" +
//                            "h: "+h+" m:"+m);
                    //把整数转化为时间
                    //            Date date = new Date();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    try {
                        Date date = simpleDateFormat.parse(h + ":" + m + "");
                        String simDate = simpleDateFormat.format(date);
//                        Log.e("TAG", "date: "+simpleDateFormat.format(date));
                        alarmPos.time = simDate;
                        baseAdapter.notifyDataSetChanged();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    alarmClass alarmtime = alarmList.get(pos);
                    String[] split = alarmtime.time.split(":");
                    int h2 = Integer.parseInt(split[0]);
                    int m2 = Integer.parseInt(split[1]);
                    cal.set(Calendar.HOUR_OF_DAY, h2);
                    cal.set(Calendar.MINUTE, m2);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    saveData();
                }
            }
        } else if (requestCode == 2) {
            alarmClass alarm = new alarmClass();
//            alarm.
            if (data != null) {
                Bundle bundle = data.getExtras();
                if (!bundle.isEmpty()) {
                    int h = data.getExtras().getInt("h");
                    int m = data.getExtras().getInt("m");
//                    Log.e("TAG", "requestCode: "+requestCode+"\nresultCode:  "+resultCode+"\n" +
//                            "h: "+h+" m:"+m);
                    //把整数转化为时间
                    //            Date date = new Date();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    try {
                        Date date = simpleDateFormat.parse(h + ":" + m + "");
                        String simDate = simpleDateFormat.format(date);
//                        Log.e("TAG", "date: "+simpleDateFormat.format(date));
                        alarm.time = simDate;
                        alarmList.add(alarm);
                        baseAdapter.notifyDataSetChanged();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    saveData();
                }
            }


        }
    }

    private void saveData() {
        //存储数据
        Gson gson = new Gson();
        String strJson = gson.toJson(alarmList);
        Log.e("TAG", "strJson: " + strJson);
        edit.putString("timeJson", strJson);
        edit.commit();
    }

    public void alarmSetting() {
        AlarmService alarmService = new AlarmService();
        alarmIntent = new Intent(this, AlarmService.class);
//        Intent intent = new Intent("abc");

//        pi = PendingIntent.getService(getApplicationContext(),0,intent,0);
//        pi = PendingIntent.getBroadcast(getApplicationContext(),0,intent,0);
        alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
        cal = Calendar.getInstance();
        //设置日历具体时间
//        cal.set(Calendar.HOUR_OF_DAY,16);
//        cal.set(Calendar.MINUTE,27);
        //测试--把data格式化 yyyy-MM--dd
//        SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//        String format = sim.format(cal.getTime());
//        Log.e("TAG", "Calendar: "+format);

//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),5*1000,pi);
//        alarmManager.set(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis()+3*1000,pi);
//
    }

}
