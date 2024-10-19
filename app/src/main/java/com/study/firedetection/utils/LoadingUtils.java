package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.study.firedetection.R;

public class LoadingUtils {
    private final AlertDialog dialog;

    public LoadingUtils(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        builder.setCancelable(false);

        this.dialog = builder.create();
    }

    public void showLoadingDialog() {
        dialog.show();
    }

    public void hideLoadingDialog() {
        dialog.dismiss();
    }
}
