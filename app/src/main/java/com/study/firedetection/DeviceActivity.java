package com.study.firedetection;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
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


public class DeviceActivity extends AppCompatActivity {
    private String DEVICE_ID;
    private final Map<DatabaseReference, ValueEventListener> events = new HashMap<>();
    private final Date CURRENT_DATE = DateUtils.getDate(new Date());
    private final Date selectedDate = new Date(this.CURRENT_DATE.getTime());
    private final List<ImageView> LIST_CONTAINER = new ArrayList<>(3);
    private boolean flagReady = false, flagDetected = false;
    private LinearLayout layoutNotifying;
    private HorizontalScrollView layoutDetecting;
    private ImageView ivNotification, btnBackward, btnForward;
    private TextView tvNotification, tvDate;
    private HistoryRecyclerAdapter historyRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getIntentData();
        this.onReady();
        this.onEvent();
    }

    private void getIntentData() {
        this.DEVICE_ID = getIntent().getStringExtra("deviceId");
    }

    private void onReady() {
        this.layoutNotifying = findViewById(R.id.layout_notifying);
        this.layoutDetecting = findViewById(R.id.layout_detecting);
        this.ivNotification = findViewById(R.id.iv_notification);
        this.tvNotification = findViewById(R.id.tv_notification);
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_1));
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_2));
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_3));
        this.btnBackward = findViewById(R.id.btn_backward);
        this.btnForward = findViewById(R.id.btn_forward);
        this.tvDate = findViewById(R.id.tv_date);
        // HISTORY LAYOUT.
        RecyclerView rvHistory = findViewById(R.id.rv_history);
        this.historyRecyclerAdapter = new HistoryRecyclerAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvHistory.setLayoutManager(layoutManager);
        rvHistory.setAdapter(this.historyRecyclerAdapter);
    }

    private void onEvent() {
        // CALENDAR EVENT.
        this.tvDate.setText(DateUtils.format(this.selectedDate));
        this.btnBackward.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format(this.selectedDate));
            this.filterHistory();
        });
        this.btnForward.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            this.selectedDate.setTime(calendar.getTimeInMillis());
            this.tvDate.setText(DateUtils.format(this.selectedDate));
            this.filterHistory();
        });
        this.tvDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.selectedDate);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    DeviceActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        this.selectedDate.setTime(calendar.getTimeInMillis());
                        this.tvDate.setText(DateUtils.format(this.selectedDate));
                        this.filterHistory();
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        // FIREBASE EVENT.
        String detectedPath = String.format("devices/%s/detect", this.DEVICE_ID);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference detectRef = database.getReference(detectedPath);
        ValueEventListener detectListener = detectRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean detected = snapshot.getValue(Boolean.class);
                flagDetected = detected != null ? detected : false;
                changeLayout(flagDetected);

                if (!flagReady) {
                    flagReady = true;
                    String capturedPath = String.format("devices/%s/captured/%s",
                            DEVICE_ID, DateUtils.format2(selectedDate));
                    DatabaseReference capturedRef = database.getReference(capturedPath);
                    ValueEventListener capturedListener = capturedRef.limitToLast(1)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    // UPDATE DETECT LAYOUT.
                                    if (flagDetected && snapshot.exists()) {
                                        LIST_CONTAINER.forEach(container -> container.setImageDrawable(null));
                                        DataSnapshot lastCapture = snapshot.getChildren().iterator().next();

                                        int index = 0;
                                        for (DataSnapshot childNode : lastCapture.getChildren()) {
                                            String imageURL = childNode.getValue(String.class);
                                            ImageView captureContainer = LIST_CONTAINER.get(index);

                                            Glide.with(DeviceActivity.this)
                                                    .load(imageURL)
                                                    .apply(new RequestOptions().transform(new RoundedCorners(20)))
                                                    .into(captureContainer);
                                            captureContainer.setOnClickListener(v -> {
                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                Uri imageUri = Uri.parse(imageURL);
                                                intent.setDataAndType(imageUri, "image/*");
                                                if (intent.resolveActivity(getPackageManager()) != null) {
                                                    startActivity(intent);
                                                }
                                            });
                                            index++;
                                        }
                                    }
                                    // UPDATE HISTORY LAYOUT.
                                    filterHistory();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                    events.put(capturedRef, capturedListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        this.events.put(detectRef, detectListener);
    }

    private void changeLayout(boolean detected) {
        if (detected) {
            if (!this.selectedDate.equals(this.CURRENT_DATE)) {
                this.selectedDate.setTime(this.CURRENT_DATE.getTime());
                this.historyRecyclerAdapter.setData(new ArrayList<>());
            }
            this.tvDate.setText(DateUtils.format(this.selectedDate));

            int color = ContextCompat.getColor(this, R.color.fire);
            this.layoutNotifying.setBackgroundColor(color);
            this.layoutDetecting.setVisibility(View.VISIBLE);
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_fire));
            this.tvNotification.setText(ContextCompat.getString(this, R.string.fire_notification));
            this.tvNotification.setTextColor(color);
        } else {
            int color = ContextCompat.getColor(this, R.color.none);
            this.layoutNotifying.setBackgroundColor(color);
            this.layoutDetecting.setVisibility(View.GONE);
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_none));
            this.tvNotification.setText(ContextCompat.getString(this, R.string.none_notification));
            this.LIST_CONTAINER.forEach(container -> container.setImageDrawable(null));
            this.tvNotification.setTextColor(color);
        }
    }

    private void filterHistory() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String capturedPath = String.format("devices/%s/captured/%s",
                this.DEVICE_ID, DateUtils.format2(this.selectedDate));
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
            historyRecyclerAdapter.setData(listItem);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.events.forEach(Query::removeEventListener);
    }
}