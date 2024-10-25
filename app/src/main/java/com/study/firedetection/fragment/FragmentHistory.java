package com.study.firedetection.fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.DeviceActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.HistoryRecyclerAdapter;
import com.study.firedetection.entity.HistoryItem;
import com.study.firedetection.utils.DateUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FragmentHistory extends Fragment {
    private final Date selectedDate = new Date(DateUtils.CURRENT_DATE.getTime());
    private Context mContext;
    private ProgressBar loadingView;
    private ImageView ivPrior, ivLater;
    private TextView tvDate, tvStartTime, tvLimitTime;
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
        this.loadingView = view.findViewById(R.id.loading_view);
        this.tvDate = view.findViewById(R.id.tv_date);
        this.ivPrior = view.findViewById(R.id.iv_prior);
        this.ivLater = view.findViewById(R.id.iv_later);
        this.tvStartTime = view.findViewById(R.id.tv_start_time);
        this.tvLimitTime = view.findViewById(R.id.tv_limit_time);
        // HISTORY LAYOUT.
        this.historyRecyclerAdapter = new HistoryRecyclerAdapter(this.mContext);
        RecyclerView rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false));
        rvHistory.setAdapter(this.historyRecyclerAdapter);
    }

    @SuppressLint("DefaultLocale")
    private void onEvent() {
        // DATE EVENT.
        this.tvDate.setText(DateUtils.format1(this.selectedDate));
        this.tvDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this.mContext, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                this.selectedDate.setTime(calendar.getTimeInMillis());
                this.tvDate.setText(DateUtils.format1(this.selectedDate));
                this.updateHistory();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        this.ivPrior.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format1(this.selectedDate));
            this.updateHistory();
        });
        this.ivLater.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format1(this.selectedDate));
            this.updateHistory();
        });
        // TIME EVENT.
        this.tvStartTime.setOnClickListener(v -> {
            int[] components = Arrays.stream(this.tvStartTime.getText().toString().split(":"))
                    .mapToInt(Integer::valueOf).toArray();

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this.mContext, (view, hourOfDay, minute) -> {
                String startTime = String.format("%02d:%02d", hourOfDay, minute);
                String limitTime = this.tvLimitTime.getText().toString();
                if (startTime.compareTo(limitTime) > 0) {
                    this.tvLimitTime.setText(ContextCompat.getString(this.mContext, R.string.default_limit_time));
                }
                this.tvStartTime.setText(startTime);
                this.filterHistory();
            }, components[0], components[1], true);
            timePickerDialog.show();
        });
        this.tvLimitTime.setOnClickListener(v -> {
            int[] components = Arrays.stream(this.tvLimitTime.getText().toString().split(":"))
                    .mapToInt(Integer::valueOf).toArray();

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this.mContext, (view, hourOfDay, minute) -> {
                String startTime = this.tvStartTime.getText().toString();
                String limitTime = String.format("%02d:%02d", hourOfDay, minute);
                if (limitTime.compareTo(startTime) < 0) {
                    this.tvStartTime.setText(ContextCompat.getString(this.mContext, R.string.default_start_time));
                }
                this.tvLimitTime.setText(limitTime);
                this.filterHistory();
            }, components[0], components[1], true);
            timePickerDialog.show();
        });
        // FIRST LOADING.
        this.updateHistory();
        this.filterHistory();
    }

    private void updateHistory() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String capturedPath = String.format("devices/%s/captured/%s",
                DeviceActivity.DEVICE_ID, DateUtils.format2(this.selectedDate));
        DatabaseReference capturedRef = database.getReference(capturedPath);
        // ENABLE LOADING.
        this.loadingView.setVisibility(View.VISIBLE);
        capturedRef.get().addOnCompleteListener(task -> {
            // RESET TIME.
            this.tvStartTime.setText(ContextCompat.getString(this.mContext, R.string.default_start_time));
            this.tvLimitTime.setText(ContextCompat.getString(this.mContext, R.string.default_limit_time));
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
                Collections.reverse(listItem);
                this.historyRecyclerAdapter.loadOriginalData(listItem);
                // DISABLE LOADING.
                this.loadingView.setVisibility(View.GONE);
            }
        });
    }

    private void filterHistory() {
        // ENABLE LOADING.
        this.loadingView.setVisibility(View.VISIBLE);
        // GET DATA.
        String startTime = String.format("%s:00", this.tvStartTime.getText().toString());
        String limitTime = String.format("%s:59", this.tvLimitTime.getText().toString());
        List<HistoryItem> filteredData = this.historyRecyclerAdapter.getOriginalData().stream()
                .filter(item -> {
                    String timestamp = item.getTimestamp();
                    return timestamp.compareTo(startTime) >= 0 && timestamp.compareTo(limitTime) <= 0;
                }).collect(Collectors.toList());
        this.historyRecyclerAdapter.loadFilteredData(filteredData);
        // DISABLE LOADING.
        this.loadingView.setVisibility(View.GONE);
    }
}