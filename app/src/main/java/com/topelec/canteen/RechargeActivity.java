package com.topelec.canteen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.topelec.buscard.CardActivityGroup;
import com.topelec.database.DatabaseHelper;

import java.text.DecimalFormat;

import it.moondroid.coverflowdemo.R;

public class RechargeActivity extends Activity {
    private Button btnAuthor;
    private Button btnCancelAuthor;
    private Button btnRecharge;
    private String currentId = new String();
    private String oldId = new String();
    private EditText rechargeText;
    private TextView rechargeRemaining;
    /**数据库相关**/
    Context mContext;
    DatabaseHelper mDatabaseHelper;
    SQLiteDatabase mDatabase;

    private final static String TABLE_NAME = "HFCard";
    private final static String ID = "_id";
    private final static String CARD_ID = "card_id";
    private final static String SUM = "sum";

    /***接收Group发送来的广播数据，同步更新UI***/
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int what = intent.getExtras().getInt("what");
            switch (what) {
                case 1://初始化错误
                    //TODO:
                    currentId = null;
                    oldId = null;
                    break;
                case 2://未检测到卡
                    rechargeText.setText("");
                    currentId = null;
                    oldId = null;
                    break;
                case 3: //成功获取卡号
                    currentId = intent.getExtras().getString("Result");
                    if (currentId == null) {
//                        statusView.setImageDrawable(getResources().getDrawable(R.drawable.standby));
                    }else {
                        // if (!currentId.equals(oldId)) { //检测到不同的卡
                        //TODO:查询数据库，存在：succeed；不存在：未授权
                        updateCardUI(currentId);
                        oldId = currentId;
                        //    } else {
                        //TODO:相同的卡，不做处理
                        //    }

                    }

                    break;
                default:
                    break;
            }


        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canteen_recharge);
        rechargeText=(EditText)findViewById(R.id.editText_recharge);
        rechargeRemaining=(TextView)findViewById(R.id.recharge_remain_text);
        /**数据库相关变量初始化**/
        mContext = this;
        mDatabaseHelper = DatabaseHelper.getInstance(mContext);
        mDatabase = mDatabaseHelper.getReadableDatabase();

        btnAuthor = (Button) findViewById(R.id.register_confirm_btn);
        btnAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(getApplicationContext(), "尝试开卡",Toast.LENGTH_SHORT).show();
                //TODO:判断没有，则创建一条数据：statusView中显示状态；若存在，则直接在statusView中显示状态
                Log.v("DEV","点击了开卡");
                if (currentId == null || currentId.length() == 0) {
                    Toast.makeText(getApplicationContext(), "未检测到卡！", Toast.LENGTH_SHORT).show();
                    return;
                }
                String result = searchHFCard(CARD_ID,currentId);
                if ( result == null ) {
                    //TODO:插入新行
                    if (insertHFCard(CARD_ID,currentId) != -1) {
                        updateCardUI(currentId);
                        Toast.makeText(getApplicationContext(), "开卡成功！卡号："+currentId, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "开卡失败！"+currentId, Toast.LENGTH_SHORT).show();
                    };
                } else if ( result.equals("-1")) {
                    //TODO:查询到多行，错误

                } else {
                    //TODO:本卡已授权
                    Toast.makeText(getApplicationContext(), "本卡已授权！" + currentId, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnCancelAuthor = (Button)findViewById(R.id.delete_card_btn);

        btnCancelAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO:跳出提示框，同意直接删除table中对应的记录条目
                if (currentId == null || currentId.length() == 0) {
                    Toast.makeText(getApplicationContext(), "未检测到卡！", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(RechargeActivity.this);
                builder.setTitle(getResources().getString(R.string.buscard_if_cancel_item));
                builder.setPositiveButton(getResources().getString(R.string.buscard_OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //TODO:删除记录
                        if (deleteHFCard(CARD_ID,currentId) != 0) {
                            Toast.makeText(getApplicationContext(), "销卡成功！", Toast.LENGTH_SHORT).show();
                            updateCardUI(currentId);
                        }else {
                            Toast.makeText(getApplicationContext(), "销卡失败！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.buscard_CANCEL), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //TODO:这里添加点击确定后的逻辑
                        Toast.makeText(getApplicationContext(), "取消消除此卡记录！", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.create().show();
            }
        });
        btnRecharge = (Button) findViewById(R.id.recharge_confirm_btn);
        btnRecharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: 获取text输入值，加入数据库sum中，并更新sumView中的结果。

                CharSequence value = rechargeText.getText();

                if (currentId == null || currentId.length() == 0 || value == null || value.length() == 0) {
                    return;
                }
                String result = searchHFCard(CARD_ID,currentId);
                if ( result == null) {
                    Toast.makeText(getApplicationContext(), "请先开卡授权！", Toast.LENGTH_SHORT).show();
                    return;

                }else {

                    String newSum = updateHFCard(CARD_ID,currentId,SUM,String.valueOf(value));
                    if ( newSum == null) {
                        Toast.makeText(getApplicationContext(), "充值失败！", Toast.LENGTH_SHORT).show();

                    }else {
                        updateCardUI(currentId);
                        Toast.makeText(getApplicationContext(), "充值成功！金额是："+newSum+"元", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });

    }
    /**
     * 查询一条记录
     * @param key
     * @param selectionArgs
     * @return
     */
    private String searchHFCard(String key,String selectionArgs) {
        Cursor cursor = mDatabase.query(TABLE_NAME, new String[]{SUM}, key + "=?", new String[] {selectionArgs}, null, null,null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        //double[] sumList = new double[cursor.getCount()];
        if (cursor.getCount() == 1) {
            double sum = cursor.getDouble(0);
            cursor.close();
            return Double.toString(sum);
        }else if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            for (int i = 0;i <cursor.getCount();i++)
            {
                cursor.moveToNext();
            }
            cursor.close();
            return "-1";
        }
    }
    /**
     * 插入一条记录
     * @param key   需要插入的列名称
     * @param data  对应列赋值
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    private long insertHFCard(String key,String data) {
        ContentValues values = new ContentValues();
        values.put(key,data);
        return mDatabase.insert(TABLE_NAME,null,values);
    }
    /**
     * 删除一条记录
     * @param key
     * @param data
     * @return 返回所删除的行数，否则返回0。
     */
    private int deleteHFCard(String key, String data) {
        return mDatabase.delete(TABLE_NAME,key + "=?", new String[] {data});
    }

    /**
     * 更新一条记录
     * @param key
     * @param data
     * @return 返回充值后的金额金额字符串，错误返回null
     */
    private String updateHFCard(String key, String data,String Column, String value) {
        ContentValues values = new ContentValues();
        String oldSum = searchHFCard(key,data);
        if (oldSum != null && !oldSum.equals("-1")) {
            double sum = Double.valueOf(oldSum) + Double.valueOf(value);
            values.put(Column, sum);
            int result =  mDatabase.update(TABLE_NAME, values, key + "=?",new String[]{data});
            if (result != 0) {
                return Double.toString(sum);
            }
        }

        return null;
    }
    @Override
    protected void onStart() {
        super.onStart();
        /**用于接收group发送过来的广播**/
        IntentFilter filter = new IntentFilter(CanteenActivityGroup.recharge_action);
        registerReceiver(mBroadcastReceiver,filter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateCardUI(currentId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }
    private void updateCardUI(String CardId) {
        String searchResult = searchHFCard(CARD_ID,CardId);
        if (searchResult == null || searchResult.length() <= 0) { //如果数据库中没有记录
//            showMsgPage(R.drawable.buscard_consume_check_wrong,getResources().getString(R.string.buscard_please_author_first),"","");
            Log.v("DEV","无记录");
            rechargeRemaining.setText("余额："+"NULL");
        } else if (searchResult.equals("-1")) {  //返回值为-1，数据库中搜索不止一个记录，错误
            Log.v("DEV","多条记录");
            rechargeRemaining.setText("余额："+"NULL");
        } else {  //返回金额，更新UI
            double newSum = Double.valueOf(searchResult) ;
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            String formattedValue = decimalFormat.format(newSum);
            rechargeRemaining.setText("余额："+formattedValue+" 元");

        }
    }
}
