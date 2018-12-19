package com.hwbluesky.weimai.wificonnectesp;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by ling on 16/12/13.
 */

public class DialogUtil {
    private DialogUtil(){}

    public static void showAlertDialog(Context context,String title,String message){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if(!TextUtils.isEmpty(title)){
            builder.setTitle(title);
        }
        if(!TextUtils.isEmpty(message)){
            builder.setMessage(message);
        }
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }
}
