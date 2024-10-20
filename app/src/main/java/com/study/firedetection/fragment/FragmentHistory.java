package com.study.firedetection.fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.study.firedetection.DeviceActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.HistoryRecyclerAdapter;
import com.study.firedetection.entity.HistoryItem;
import com.study.firedetection.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentHistory extends Fragment {
    private final Map<DatabaseReference, ValueEventListener> FIREBASE_EVENTS = new HashMap<>();
    private final Date selectedDate = new Date(DateUtils.CURRENT_DATE.getTime());
    private Context mContext;
    private TextView tvDate;
    private ImageView btnBackward, btnForward;
    private HistoryRecyclerAdapter historyRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.onReady(view);
        this.onEvent();
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.tvDate = view.findViewById(R.id.tv_date);
        this.btnBackward = view.findViewById(R.id.btn_backward);
        this.btnForward = view.findViewById(R.id.btn_forward);
        // HISTORY LAYOUT.
        RecyclerView rvHistory = view.findViewById(R.id.rv_history);
        this.historyRecyclerAdapter = new HistoryRecyclerAdapter(this.mContext);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false);
        rvHistory.setLayoutManager(layoutManager);
        rvHistory.setAdapter(this.historyRecyclerAdapter);
    }

    private void onEvent() {
        this.tvDate.setText(DateUtils.format(this.selectedDate));
        this.btnBackward.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format(this.selectedDate));
            this.updateHistory();
        });
        this.btnForward.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format(this.selectedDate));
            this.updateHistory();
        });
        this.tvDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this.mContext, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                this.selectedDate.setTime(calendar.getTimeInMillis());
                this.tvDate.setText(DateUtils.format(this.selectedDate));
                this.updateHistory();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        this.updateHistory();
        // FIREBASE EVENT.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String capturedPath = String.format("devices/%s/captured", DeviceActivity.DEVICE_ID);
        DatabaseReference capturedRef = database.getReference(capturedPath);
        ValueEventListener capturedListener = capturedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        this.FIREBASE_EVENTS.put(capturedRef, capturedListener);
    }

    private void updateHistory() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String capturedPath = String.format("devices/%s/captured/%s",
                DeviceActivity.DEVICE_ID, DateUtils.format2(this.selectedDate));
        DatabaseReference capturedRef = database.getReference(capturedPath);
        capturedRef.get().addOnCompleteListener(task -> {
            List<HistoryItem> listItem = new ArrayList<>();

            for (DataSnapshot dataTimestamp : task.getResult().getChildren()) {
                String timestamp = dataTimestamp.getKey();
                List<String> listURL = new ArrayList<>();
                dataTimestamp.getChildren().forEach(dataCapture ->
                        listURL.add(dataCapture.getValue(String.class)));
                listItem.add(new HistoryItem(timestamp, listURL));
            }

            Collections.reverse(listItem);
            this.historyRecyclerAdapter.setData(listItem);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.FIREBASE_EVENTS.forEach(Query::removeEventListener);
    }
}