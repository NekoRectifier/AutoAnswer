package xyz.sbfd.autoanswer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    SharedPreferences sharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent1) {
        sharedPreferences = context.getSharedPreferences("app",Context.MODE_PRIVATE);
        Log.i("AlarmReceiver", "onReceive: dnmd");

        Toast.makeText(context, "自动打卡！", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(sharedPreferences.getString("uri","")));
        intent.setType("image/*");
        intent.setPackage("com.tencent.mobileqq");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "----WDNMD这是专用分享----").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        //context.startActivity(intent);
        Toast.makeText(context, "...不要操作手机...", Toast.LENGTH_SHORT).show();

    }
}
