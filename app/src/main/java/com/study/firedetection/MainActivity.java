package com.study.firedetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private final List<ImageView> LIST_CONTAINER = new ArrayList<>(3);
    private boolean flagReady = false, flagDetected = false;
    private LinearLayout layoutNotifying;
    private HorizontalScrollView layoutDetecting;
    private ImageView ivNotification;
    private TextView tvNotification;
    private HistoryRecyclerAdapter historyRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(new SplashScreen.OnExitAnimationListener() {
            @Override
            public void onSplashScreenExit(@NonNull SplashScreenViewProvider splashScreenViewProvider) {
                new Handler().postDelayed(() -> {
                    splashScreenViewProvider.remove();
                    findViewById(R.id.layout_main).startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.entrance));
                }, 3000);
            }
        });
        setContentView(R.layout.activity_main);
        this.onReady();
        this.onEvent();
        this.requestPermission();
    }

    private void onReady() {
        this.layoutNotifying = findViewById(R.id.layout_notifying);
        this.layoutDetecting = findViewById(R.id.layout_detecting);
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
                        @SuppressLint("SimpleDateFormat")
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<HistoryItem> listItem = new ArrayList<>();
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            SimpleDateFormat desFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                            snapshot.getChildren().forEach(dataTimestamp -> {
                                List<String> listURL = new ArrayList<>();
                                String timestamp = dataTimestamp.getKey();
                                try {
                                    timestamp = desFormat.format(srcFormat.parse(dataTimestamp.getKey()));
                                } catch (ParseException ignored) {
                                }

                                dataTimestamp.getChildren().forEach(dataCapture ->
                                        listURL.add(dataCapture.getValue(String.class)));
                                listItem.add(new HistoryItem(timestamp, listURL));
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

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
}