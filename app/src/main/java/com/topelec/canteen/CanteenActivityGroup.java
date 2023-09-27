package com.topelec.canteen;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.topelec.buscard.CardActivityGroup;
import com.topelec.rfidcontrol.ModulesControl;
import com.topelec.zigbeecontrol.Command;
import com.topelec.zigbeecontrol.SensorControl;

import it.moondroid.coverflowdemo.R;

public class CanteenActivityGroup extends ActivityGroup implements SensorControl.LedListener {
    public static final String resume_action = "com.topelec.canteen.resume_action";
    public static final String recharge_action = "com.topelec.canteen.recharge_action";
    private FrameLayout bodyView;
    ModulesControl mModulesControl;
    private int flag;
    SensorControl mSensorControl;
    Intent intent;
    private boolean isLed1On=false;
    private boolean isLed2On=false;
    private boolean isLed3On=false;
    private boolean isLed4On=false;
    Handler timerHandler = new Handler();
    Runnable sendRunnable = new Runnable() {
        int i = 1;
        @Override
        public void run() {

            timerHandler.postDelayed(this,Command.CHECK_SENSOR_DELAY);
        }
    };
    /**
     * 用于更新rechargeUI
     */
    Handler uiHandler = new Handler() {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            Bundle data;
            if (flag == 0) {//resume
                intent = new Intent(resume_action);
            }else if (flag == 1){
                intent = new Intent(recharge_action);
            }

//            intent = new Intent(recharge_action);
            switch (msg.what) {
                //判断发送的消息
                case Command.HF_TYPE:  //设置卡片类型TypeA返回结果  ,错误类型:1
                    data = msg.getData();
                    if (data.getBoolean("result") == false) {
                        intent.putExtra("what",1);
                        intent.putExtra("Result",getResources().getString(R.string.buscard_type_a_fail));
                        sendBroadcast(intent);
                    }
                    break;
                case  Command.HF_FREQ:  //射频控制（打开或者关闭）返回结果   ,错误类型:1
                    data = msg.getData();
                    if (data.getBoolean("result") == false) {
                        intent.putExtra("what",1);
                        if (data.getBoolean("Result")) {
                            intent.putExtra("Result",getResources().getString(R.string.buscard_frequency_open_fail));
                        }else {
                            intent.putExtra("Result",getResources().getString(R.string.buscard_frequency_close_fail));
                        }
                        sendBroadcast(intent);
                    }

                    break;
                case Command.HF_ACTIVE:       //激活卡片，寻卡，返回结果
                    data = msg.getData();
                    if (data.getBoolean("result")) {
//                        hfView.setText(R.string.active_card_succeed);
                    } else {
                        intent.putExtra("what",2);
                        sendBroadcast(intent);

                    }

                    break;
                case Command.HF_ID:      //防冲突（获取卡号）返回结果

                    data = msg.getData();
                    intent.putExtra("what",3);

                    if (data.getBoolean("result")) {
                        intent.putExtra("Result",data.getString("cardNo"));
                        sendBroadcast(intent);
                    } else {

                    }
//                    Log.v(TAG,"Result = "+ data.getString("cardNo"));

                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    /**
     * 用于更新UI
     */
    Handler myHandler = new Handler() {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            Bundle data;
            data = msg.getData();
            switch (msg.what) {
                //判断发送的消息
                case 0x01:
                    switch (data.getByte("led_id")) {
                        case 0x01:
                            if (data.getByte("led_status") == 0x01) {
                                isLed1On = true;
                            }else {
                                isLed1On = false;
                            }
                            break;
                        case 0x02:
                            if (data.getByte("led_status") == 0x01) {
                                isLed2On = true;
                            }else {
                                isLed2On = false;
                            }
                            break;
                        case 0x03:
                            if (data.getByte("led_status") == 0x01) {
                                isLed3On = true;
                            } else {
                                isLed3On = false;
                            }
                            break;
                        case 0x04:
                            if (data.getByte("led_status") == 0x01) {
                                isLed4On = true;
                            }else {
                                isLed4On = false;
                            }
                            break;
                        case 0x05:
                            if (data.getByte("led_status") == 0x01) {
                                isLed1On = true;
                                isLed2On = true;
                                isLed3On = true;
                                isLed4On = true;
                            } else {
                                isLed1On = false;
                                isLed2On = false;
                                isLed3On = false;
                                isLed4On = false;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canteen_group);
        mSensorControl=new SensorControl();
        Log.v("DEV","创建了mSensorControl");
        mSensorControl.addLedListener(this);
        mModulesControl = new ModulesControl(uiHandler);
        mModulesControl.actionControl(true);
        initMainView();
        this.switchToRecharge();

    }
    /*
     * 初始化主界面底部的功能菜单;
     */
    public void initMainView() {

        bodyView=(FrameLayout) findViewById(R.id.body);
        bodyView = (FrameLayout) findViewById(R.id.body);

        Button btnRecharge = (Button) findViewById(R.id.recharge_btn);
        Button btnResume = (Button) findViewById(R.id.resume_btn);

        btnRecharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("DEV","切换到充值模式");
                switchToRecharge();
                flag=1;
            }
        });

        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("DEV","消费模式");
                switchToResume();
                flag=0;
            }
        });
    }
    private void switchToRecharge() {
        Intent intent = new Intent(CanteenActivityGroup.this, RechargeActivity.class);
        View subPageView = getLocalActivityManager().startActivity("Recharge", intent).getDecorView();
        bodyView.removeAllViews();
        bodyView.addView(subPageView);
        if (isLed2On)
        {
            mSensorControl.led2_Off(false);
        }else{
            mSensorControl.led2_On(false);
        }
        if (isLed1On)
        {
            mSensorControl.led1_On(false);
        }else{
            mSensorControl.led1_Off(false);
        }
        Toast.makeText(this, "充值模式", Toast.LENGTH_SHORT).show();
    }

    private void switchToResume() {
        Intent intent = new Intent(CanteenActivityGroup.this, ResumeActivity.class);
        View subPageView = getLocalActivityManager().startActivity("Resume", intent).getDecorView();
        bodyView.removeAllViews();
        bodyView.addView(subPageView);
        if (isLed1On)
        {
            mSensorControl.led1_Off(false);
        }else{
            mSensorControl.led1_On(false);
        }
        if (isLed2On)
        {
            mSensorControl.led2_On(false);
        }else{
            mSensorControl.led2_Off(false);
        }
        Toast.makeText(this, "消费模式", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onStart() {
        super.onStart();

        mSensorControl.actionControl(true);

        //TODO:每350ms发送一次数据
        timerHandler.postDelayed(sendRunnable, Command.CHECK_SENSOR_DELAY);
    }
    @Override
    protected void onStop() {

        super.onStop();
        timerHandler.removeCallbacks(sendRunnable);
        mSensorControl.actionControl(false);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSensorControl.removeLedListener(this);
        mSensorControl.closeSerialDevice();
    }

    public void LedControlResult(byte led_id,byte led_status){
        Message msg = new Message();
        msg.what = 0x01;
        Bundle data = new Bundle();
        data.putByte("led_id",led_id);
        data.putByte("led_status",led_status);
        msg.setData(data);
        Log.v("DEV","hhh");
        Log.v("DEV",msg.getData().toString());
        myHandler.sendMessage(msg);
    }
}
