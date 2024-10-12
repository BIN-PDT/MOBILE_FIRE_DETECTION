package com.study.firedetection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private final List<ImageView> LIST_CONTAINER = new ArrayList<>(3);
    private boolean flagReady = false, flagDetected = false;
    private LinearLayout mainLayout;
    private ImageView ivNotification;
    private TextView tvNotification;
    private HistoryRecyclerAdapter historyRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.onReady();
        this.onEvent();
        this.requestPermission();
    }

    private void onReady() {
        this.mainLayout = findViewById(R.id.main);
        this.ivNotification = findViewById(R.id.iv_notification);
        this.tvNotification = findViewById(R.id.tv_notification);
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_1));
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_2));
        this.LIST_CONTAINER.add(findViewById(R.id.iv_capture_3));
        // HISTORY LAYOUT.
        RecyclerView rvHistory = findViewById(R.id.rv_history);
        this.historyRecyclerAdapter = new HistoryRecyclerAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvHistory.setLayoutManager(layoutManager);
        rvHistory.setAdapter(this.historyRecyclerAdapter);
    }

    private void onEvent() {
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
                    DatabaseReference capturedRef = database.getReference("captured");
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
                            List<HistoryItem> listItem = new ArrayList<>();
                            snapshot.getChildren().forEach(dataTimestamp -> {
                                List<String> listURL = new ArrayList<>();
                                dataTimestamp.getChildren().forEach(dataCapture ->
                                        listURL.add(dataCapture.getValue(String.class)));
                                listItem.add(new HistoryItem(dataTimestamp.getKey(), listURL));
                            });
                            Collections.reverse(listItem);
                            historyRecyclerAdapter.setData(listItem);
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
            this.mainLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_fire));
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_fire));
            this.tvNotification.setText(ContextCompat.getString(this, R.string.fire_notification));
        } else {
            this.mainLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_none));
            this.ivNotification.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_none));
            this.tvNotification.setText(ContextCompat.getString(this, R.string.none_notification));
            this.LIST_CONTAINER.forEach(container -> container.setImageDrawable(null));
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
}