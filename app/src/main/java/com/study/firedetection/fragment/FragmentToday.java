package com.study.firedetection.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.study.firedetection.DeviceActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.HistoryRecyclerAdapter;
import com.study.firedetection.entity.HistoryItem;
import com.study.firedetection.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FragmentToday extends Fragment {
    private Context mContext;
    private ProgressBar loadingView;
    private RelativeLayout layoutNotifying;
    private ImageView ivBack, ivNotification;
    private TextView tvNotification;
    private HistoryRecyclerAdapter historyRecyclerAdapter;
    private String lastCaptureTimestamp = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.onReady(view);
        this.onEvent();
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.loadingView = view.findViewById(R.id.loading_view);
        this.layoutNotifying = view.findViewById(R.id.layout_notifying);
        this.ivBack = view.findViewById(R.id.iv_back);
        this.ivNotification = view.findViewById(R.id.iv_notification);
        this.tvNotification = view.findViewById(R.id.tv_notification);
        // HISTORY LAYOUT.
        this.historyRecyclerAdapter = new HistoryRecyclerAdapter(this.mContext);
        RecyclerView rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false));
        rvHistory.setAdapter(this.historyRecyclerAdapter);
    }

    private void onEvent() {
        this.ivBack.setOnClickListener(v -> requireActivity().finish());
        // FIREBASE EVENT.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String detectedPath = String.format("devices/%s/detect", DeviceActivity.DEVICE_ID);
        DatabaseReference detectRef = database.getReference(detectedPath);
        ValueEventListener detectListener = detectRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean detected = snapshot.getValue(Boolean.class);
                changeLayout(detected != null && detected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        DeviceActivity.FIREBASE_EVENTS.put(detectRef, detectListener);

        String capturedPath = String.format("devices/%s/captured/%s",
                DeviceActivity.DEVICE_ID, DateUtils.CURRENT_DATE_TEXT_2);
        DatabaseReference capturedRef = database.getReference(capturedPath);
        capturedRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // GET DATA.
                List<HistoryItem> listItem = new ArrayList<>();
                for (DataSnapshot dataTimestamp : task.getResult().getChildren()) {
                    String timestamp = dataTimestamp.getKey();
                    List<String> listURL = new ArrayList<>();
                    dataTimestamp.getChildren().forEach(dataCapture ->
                            listURL.add(dataCapture.getValue(String.class)));
                    listItem.add(new HistoryItem(timestamp, listURL));
                }
                // REMOVE LAST ITEM AT FIRST LOADING.
                if (!listItem.isEmpty()) listItem.remove(listItem.size() - 1);
                Collections.reverse(listItem);
                this.historyRecyclerAdapter.loadOriginalData(listItem);
                // LAST CAPTURE EVENT.
                ValueEventListener lastCapturedListener = capturedRef.limitToLast(1)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    DataSnapshot lastCapture = snapshot.getChildren().iterator().next();
                                    updateHistory(lastCapture);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                DeviceActivity.FIREBASE_EVENTS.put(capturedRef, lastCapturedListener);
                // DISABLE LOADING.
                this.loadingView.setVisibility(View.GONE);
            }
        });
    }

    private void changeLayout(boolean detected) {
        if (detected) {
            int color = ContextCompat.getColor(this.mContext, R.color.fire);
            this.layoutNotifying.setBackgroundColor(color);
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this.mContext, R.drawable.icon_fire));
            this.tvNotification.setText(ContextCompat.getString(this.mContext, R.string.fire_notification));
            this.tvNotification.setTextColor(color);
        } else {
            int color = ContextCompat.getColor(this.mContext, R.color.none);
            this.layoutNotifying.setBackgroundColor(color);
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this.mContext, R.drawable.icon_none));
            this.tvNotification.setText(ContextCompat.getString(this.mContext, R.string.none_notification));
            this.tvNotification.setTextColor(color);
        }
    }

    private void updateHistory(DataSnapshot lastCapture) {
        // GET ITEM.
        String timestamp = lastCapture.getKey();
        List<String> listURL = new ArrayList<>();
        lastCapture.getChildren().forEach(dataCapture ->
                listURL.add(dataCapture.getValue(String.class)));
        HistoryItem item = new HistoryItem(timestamp, listURL);
        // GET DATA.
        List<HistoryItem> listItem = new ArrayList<>(this.historyRecyclerAdapter.getOriginalData());
        if (!listItem.isEmpty() && this.lastCaptureTimestamp.equals(timestamp)) listItem.remove(0);
        else this.lastCaptureTimestamp = timestamp;
        listItem.add(0, item);
        this.historyRecyclerAdapter.loadOriginalData(listItem);
    }
}