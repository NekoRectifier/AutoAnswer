package xyz.sbfd.autoanswer;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;
import com.canking.minipay.Config;
import com.canking.minipay.MiniPayUtils;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.app.PendingIntent.getBroadcast;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    String filePath = "";
    AlertDialog alertDialog;
    boolean permission = false;
    SharedPreferences sharedPreferences;
    Toolbar toolbar;
    Config config;
    TimePicker timePicker;
    Button start;
    Button stop;
    Button start1;
    Button stop1;
    Button select;
    int timeConfigSelector = 0;
    int minutes;
    int seconds;
    Uri uri;
    Intent intent_all;
    PendingIntent sender;
    AlarmManager alarmManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);

        intent_all = new Intent(MainActivity.this, AlarmReceiver.class);
        sender = getBroadcast(MainActivity.this, 0, intent_all, PendingIntent.FLAG_CANCEL_CURRENT);
        filePath = sharedPreferences.getString("path","");

        final EditText editText = findViewById(R.id.editText);
        editText.setText(String.valueOf(sharedPreferences.getInt("number",8)));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!editText.getText().toString().equals("")) sharedPreferences.edit().putInt("number",Integer.parseInt(editText.getText().toString())).apply();
                Toast.makeText(MainActivity.this, "数字已保存", Toast.LENGTH_SHORT).show();
            }
        });

        start = findViewById(R.id.time_sele_start);
        start.setEnabled(sharedPreferences.getBoolean("check",true));
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeConfigSelector = 1;
                initTimePicker();
            }
        });

        start.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(MainActivity.this, "已触发", Toast.LENGTH_SHORT).show();
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + 5000, sender);
                return true;
            }
        });

        stop = findViewById(R.id.time_sele_stop);
        stop.setEnabled(false);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeConfigSelector = 2;
                initTimePicker();
            }
        });

        start1 = findViewById(R.id.button4);
        start1.setEnabled(sharedPreferences.getBoolean("check",true));
        start1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeConfigSelector = 3;
                initTimePicker();
            }
        });

        stop1 = findViewById(R.id.button5);
        stop1.setEnabled(false);
        stop1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeConfigSelector = 4;
                initTimePicker();
            }
        });

        select = findViewById(R.id.button3);
        select.setText(sharedPreferences.getString("path","选择图片"));
        select.setEnabled(sharedPreferences.getBoolean("check",true));
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhoto();
            }
        });
        select.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!filePath.equals("")) {
                    Bitmap bitmap = getLocalBitmap(filePath);
                    Drawable drawable = new BitmapDrawable(bitmap);
                    ImageView imageView = new ImageView(MainActivity.this);
                    imageView.setImageDrawable(drawable);
                    AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("预览")
                            .setView(imageView)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) { }
                            })
                            .create();
                    alertDialog1.show();
                }
                return true;
            }
        });

        start.setText(sharedPreferences.getString("time_start1","00:00"));
        stop.setText(sharedPreferences.getString("time_stop1","00:00"));
        start1.setText(sharedPreferences.getString("time_start2","00:00"));
        stop1.setText(sharedPreferences.getString("time_stop2","00:00"));

        Switch autoMicro = findViewById(R.id.switch1);
        Switch autoSpeaker = findViewById(R.id.switch2);
        Switch mainSwitch = findViewById(R.id.switch3);
        Switch autoCheck = findViewById(R.id.switch4);
        switchListener(autoSpeaker,"speaker",false);
        switchListener(autoMicro,"microphone",true);
        switchListener(mainSwitch,"main",true);
        switchListener(autoCheck,"check",true);

        Log.i(TAG, "onCreate: "+serviceCheck(this,AccessibilityUIService.class.getName()));
        if(!serviceCheck(this,AccessibilityUIService.class.getName())){
            initDialog();
        }

        config = new Config.Builder("fkx1393914mhhfwahymb1a6",R.drawable.ali,R.drawable.wc).build();
    }


    public static Bitmap getLocalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void switchListener(Switch s, final String name, boolean def) {
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit().putBoolean(name,b).apply();
                if(name.equals("check")) {
                    if (!sharedPreferences.getBoolean("check", true)) {
                        start.setEnabled(false);
                        stop.setEnabled(false);
                        start1.setEnabled(false);
                        stop1.setEnabled(false);
                        select.setEnabled(false);
                    } else {
                        start.setEnabled(true);
                        //stop.setEnabled(true);
                        start1.setEnabled(true);
                        //stop1.setEnabled(true);
                        select.setEnabled(true);
                    }
                }
            }
        });
        s.setChecked(sharedPreferences.getBoolean(name,def));
    }

    public void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent,1);
    }

    public long timeFix(long time) {
        Calendar calendar = Calendar.getInstance();
        long nowTime = calendar.getTimeInMillis();
        if (time - nowTime < 0) {
            Log.i(TAG, "timeFix: 时间早于现在");
            calendar.set(Calendar.DAY_OF_MONTH,calendar.get(Calendar.DAY_OF_MONTH)+1);
            return calendar.getTimeInMillis();
        }
        return time;
    }

    public void initTimePicker(){
        timePicker = new TimePicker(MainActivity.this);
        timePicker.setIs24HourView(true);

        final Calendar calendar = Calendar.getInstance();
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setMessage("选择时间")
                .setView(timePicker)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        calendar.set(Calendar.HOUR_OF_DAY, minutes);
                        calendar.set(Calendar.MINUTE, seconds);
                        calendar.set(Calendar.SECOND,14);
                        calendar.set(Calendar.MILLISECOND, 250);
                        Log.i(TAG, "onClick: "+calendar.getTime().toString());
                        if(timeConfigSelector == 1){
                            start.setText(String.format(Locale.CHINA,"%02d:%02d",minutes,seconds));
                            sharedPreferences.edit().putString("time_start1",String.format(Locale.CHINA,"%02d:%02d",minutes,seconds)).apply();
                            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timeFix(calendar.getTimeInMillis()),AlarmManager.INTERVAL_DAY, sender);
                        } else if(timeConfigSelector == 2) {
                            stop.setText(String.format(Locale.CHINA,"%02d:%02d",minutes,seconds));
                            sharedPreferences.edit().putString("time_stop1",minutes+":"+seconds).apply();
                        } else if(timeConfigSelector == 3) {
                            start1.setText(String.format(Locale.CHINA,"%02d:%02d",minutes,seconds));
                            sharedPreferences.edit().putString("time_start2",String.format(Locale.CHINA,"%02d:%02d",minutes,seconds)).apply();
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timeFix(calendar.getTimeInMillis()),AlarmManager.INTERVAL_DAY, sender);
                        } else if(timeConfigSelector == 4) {
                            stop1.setText(String.format(Locale.CHINA,"%02d:%02d",minutes,seconds));
                            sharedPreferences.edit().putString("time_stop2",minutes+":"+seconds).apply();
                        }
                        Toast.makeText(MainActivity.this, "已设置", Toast.LENGTH_SHORT).show();
                        timeConfigSelector = 0;
                        //TODO: 时间早晚判断
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                })
                .create();
        alertDialog.show();

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                minutes = i;
                seconds = i1;
            }
        });
    }

    public boolean serviceCheck(Context context, String className) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo>
                runningServices = activityManager.getRunningServices(100);
        runningServices.size();
        for (int i = 0;i<runningServices.size();i++){
            ComponentName service = runningServices.get(i).service;
            if (service.getClassName().contains(className)){
                return true;
            }
        }
        return false;
    }

    public void initDialog(){
        alertDialog.setTitle("提示");
        alertDialog.setMessage("本应用使用'无障碍服务'来执行操作,需要您手动在设置中打开该服务");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "跳转至设置页面", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent,2);
                alertDialog.dismiss();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "不给", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && serviceCheck(MainActivity.this, AccessibilityUIService.class.getName())) {
            permission = true;
            Toast.makeText(this, "无障碍授权成功", Toast.LENGTH_SHORT).show();
        } else if (requestCode == 1 && data != null) {
            filePath = FileUtil.getFilePathByUri(this, Objects.requireNonNull(data.getData()));
            if (!TextUtils.isEmpty(filePath)) {
                Toast.makeText(this, "图片已获取,长按按钮来预览", Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString("path",filePath).apply();
                select.setText(filePath);
            }
            File file = new File(filePath);
            uri = FileProvider.getUriForFile(MainActivity.this,"xyz.sbfd.autoanswer.fileprovider",file);
            sharedPreferences.edit().putString("uri",uri.toString()).apply();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.acc:
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.this.startActivityForResult(intent,2);
                break;
            case R.id.donate:
                Snackbar.make(toolbar,"捐赠一下嘛?",Snackbar.LENGTH_LONG).setAction("好东西 捐!", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MiniPayUtils.setupPay(MainActivity.this,config);
                    }
                }).show();
                return true;
            case R.id.about:
                break;
            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
