package xyz.sbfd.autoanswer;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AccessibilityUIService extends AccessibilityService {

    int timer_num = 0;
    int Number_of_people = 0;
    int min_people = 8;
    int classification = 0;
    int shareLocker = 0;
    // 1 = 语音
    // 2 = 视频
    public boolean classing = false;
    boolean autoMicro = true;
    boolean autoSpeaker  = false;
    boolean main = true;
    boolean speakerLocker = false;
    boolean timerLocker = false;
    SharedPreferences sharedPreferences;
    String text;
    String className;
    String bigStr;
    String AppName = "";
    String TAG = "AccessibilityUIService";
    Timer timer;

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "onAccessibilityEvent: Connected!");
        sharedPreferences = getSharedPreferences("app",MODE_PRIVATE);
        min_people = sharedPreferences.getInt("number",8);
        Log.i(TAG, "onServiceConnected: minPeople:"+min_people);
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        main = sharedPreferences.getBoolean("main",true);
        if(event.getPackageName() != null) AppName = event.getPackageName().toString();
        if((AppName.equals("com.tencent.mobileqq") || AppName.equals("xyz.sbfd.autoanswer") || AppName.equals("android")) && main) {
            recycle(getRootInActiveWindow());
        }
    }

    @Override
    public void onInterrupt() {

    }
    public boolean isNumeric(String str) {
        try {
            bigStr = new BigDecimal(str).toString();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void initSharedValues(boolean b) {
        if(b){
            autoMicro = sharedPreferences.getBoolean("microphone",true);
            autoSpeaker = sharedPreferences.getBoolean("speaker",false);
        }
        if (!timerLocker) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer_num++;
                }
            },1000 * 60);
        }
        timerLocker = true;
    }

    public boolean timeCheck(Calendar calendar) {
        Date time1;
        Date time2;
        long timeDiff1 = 0;
        long timeDiff2 = 0;
        long timeDiff3 = 0;
        long timeDiff4 = 0;
        String time_start1 = sharedPreferences.getString("time_start1", "");
        String time_start2 = sharedPreferences.getString("time_start2", "");
        DateFormat format = new SimpleDateFormat("HH:mm", Locale.CHINA);
        try {
            time1 = format.parse(time_start1);
            time2 = format.parse(time_start2);
            timeDiff1 = time1.getTime() - calendar.getTimeInMillis();
            timeDiff2 =  calendar.getTimeInMillis() - time1.getTime();
            timeDiff3 = time2.getTime() - calendar.getTimeInMillis();
            timeDiff4 =  calendar.getTimeInMillis() - time2.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!(Math.abs(timeDiff1) > 1000 * 60) && !(Math.abs(timeDiff2) > 1000 * 60) && !(Math.abs(timeDiff3) > 1000 * 60) && !(Math.abs(timeDiff4) > 1000 * 60)) {
            return true;
        }
        Log.i(TAG, "timeCheck: "+timeDiff1+' '+timeDiff2+' '+timeDiff3+' '+timeDiff4);
        return false;
    }

    public void recycle(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            if (nodeInfo.getChildCount() == 0) {
//                Log.i(TAG, "onAccessibilityEvent: -------------------------------");
//                Log.i(TAG, "onAccessibilityEvent: " + nodeInfo.toString());
//                Log.i(TAG, "recycle: " + classing);
//                Log.i(TAG, "recycle: " + timer_num);
                className = nodeInfo.getClassName().toString();
                    if (!classing && nodeInfo.getText() != null) {
                        text = nodeInfo.getText().toString();

                        if(className.equals("android.widget.TextView") && text.contains("给好")) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            if(timeCheck(calendar)) {
                                nodeInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                shareLocker = 1;
                            }
                        }

                        if(shareLocker == 1 && text.contains("中学高二七班")) {
                            Log.i(TAG, "recycle: "+nodeInfo.getParent().getParent().toString());
                            nodeInfo.getParent().getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            shareLocker = 2;
                        }

                        if(shareLocker == 2 && text.equals("发送")) {
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            shareLocker = 0;
                        }

                        if (text.contains("正在语音通话")) {
                            AccessibilityNodeInfo target = nodeInfo.getParent().getParent();
                            target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Toast.makeText(this, "已进入通话界面", Toast.LENGTH_SHORT).show();
                            initSharedValues(true);
                        }

                        if (className.equals("android.widget.TextView") && text.contains("通话成员(")) {
                            int num = Integer.parseInt(text.replaceAll("\\D", ""));
                            Log.i(TAG, "recycle: " + num);
                            Number_of_people = num;
                        }

                        if (sharedPreferences.getInt("number", 8) < Number_of_people) {
                            if (className.equals("android.widget.Button") && text.contains("加入")) {
                                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                classing = true;
                                classification = 1;
                            }
                        }

                        if(text.contains("正在视频聊天")) {
                            Toast.makeText(this, "进入视频聊天", Toast.LENGTH_SHORT).show();
                            nodeInfo.getParent().getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            classing = true;
                            classification = 2;
                            initSharedValues(false);
                        }

                    } else if (classing && nodeInfo.getText() == null){
                        if (nodeInfo.getContentDescription() != null) {
                            if (nodeInfo.getContentDescription().toString().contains("关闭") && className.equals("android.widget.Button") && autoMicro) {
                                Toast.makeText(this, "已关闭麦克风", Toast.LENGTH_SHORT).show();
                                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                        }
                    } else if(classing && nodeInfo.getText() != null){
                        String str = nodeInfo.getText().toString();
                        if (className.equals("android.widget.EditText") && classification != 2) {
                            if (!str.contains("正在语音通话") || !str.contains("正在视频聊天")) {
                                Toast.makeText(this, "检测到classing状态非法,重置其数据", Toast.LENGTH_SHORT).show();
                                classing = false;
                                timer.cancel();
                            }
                        }

                        if (str.contains("主已关闭当前群聊")) {
                            Toast.makeText(this, "侦测到语音已经被关闭", Toast.LENGTH_SHORT).show();
                            classing = timerLocker = speakerLocker = false;
                            timer.cancel();
                        }

                        if (str.contains("聊天已结束")) {
                            Toast.makeText(this, "侦测到视频已经被关闭", Toast.LENGTH_SHORT).show();
                            classing = timerLocker = speakerLocker = false;
                            timer.cancel();
                        }

                        if (className.equals("android.widget.TextView") && isNumeric(text)) {
                            if (Integer.parseInt(bigStr) < 15 && timer_num > 10) {
                                //TODO: exit for qun video
                            }
                        }

                        if (className.equals("android.widget.Button") && nodeInfo.getText().toString().contains("扬声") && autoSpeaker && !speakerLocker) {
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            speakerLocker = true;
                        }
                    } else if(classing) {
                        if (timer_num > 55) {
                            if (nodeInfo.getContentDescription().toString().contains("挂") && className.equals("android.widget.Button")) {
                                Toast.makeText(this, "上课时间已达55min,已强制挂断", Toast.LENGTH_SHORT).show();
                                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                classing = timerLocker = speakerLocker = false;
                                timer.cancel();
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                        if (nodeInfo.getChild(i) != null) {
                            recycle(nodeInfo.getChild(i));
                        }
                    }
                }
            }
        }
    }