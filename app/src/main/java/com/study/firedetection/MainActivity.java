package com.study.firedetection;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import com.google.firebase.database.ValueEventListener;
import com.study.firedetection.adapter.HistoryRecyclerAdapter;
import com.study.firedetection.entity.HistoryItem;
import com.study.firedetection.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
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
        this.createNotificationChannel();
        this.requestPermission();
        this.onReady();
        this.onEvent();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = getString(R.string.channel_id);
            String name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
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
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        this.selectedDate.setTime(calendar.getTimeInMillis());
                        this.tvDate.setText(DateUtils.format(this.selectedDate));
                        this.filterHistory();
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        // FIREBASE EVENT.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference detectedRef = database.getReference("detected");
        detectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean detected = snapshot.getValue(Boolean.class);
                flagDetected = detected != null ? detected : false;
                changeLayout(flagDetected);

                if (!flagReady) {
                    flagReady = true;
                    String capturedPath = String.format("captured/%s", DateUtils.format2(selectedDate));
                    DatabaseReference capturedRef = database.getReference(capturedPath);
                    capturedRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (flagDetected && snapshot.exists()) {
                                LIST_CONTAINER.forEach(container -> container.setImageDrawable(null));
                                DataSnapshot lastCapture = snapshot.getChildren().iterator().next();

                                int index = 0;
                                for (DataSnapshot childNode : lastCapture.getChildren()) {
                                    String imageURL = childNode.getValue(String.class);
                                    ImageView captureContainer = LIST_CONTAINER.get(index);

                                    Glide.with(MainActivity.this)
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
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    capturedRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            filterHistory();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
        String capturedPath = String.format("captured/%s", DateUtils.format2(this.selectedDate));
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
}