/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.polishSignLanguage.detection;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.polishSignLanguage.detection.customview.OverlayView;
import com.polishSignLanguage.detection.env.BorderedText;
import com.polishSignLanguage.detection.env.ImageUtils;
import com.polishSignLanguage.detection.env.Logger;
import com.polishSignLanguage.detection.tracking.MultiBoxTracker;

import com.polishSignLanguage.detection.customview.OverlayView.DrawCallback;
import com.polishSignLanguage.detection.tflite.Detector;
import com.polishSignLanguage.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class LearnGestureActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.4f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Detector detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    private ArrayList<String> currentList = new ArrayList<String>();
    private ArrayList<String> readedTextArray = new ArrayList<String>();
    private boolean allEquals = true;

    private TextView textValue;
    private TextView counterView;
    private TextView remainCounterView;
    private int counter = 0;

    private int WIN = 3;

    private String gesture;
    private String expectedWord;
    private String level;
    private boolean isReminder;
    private int currentIdx;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learn_gesture);

        Intent intent = getIntent();
        isReminder = intent.getBooleanExtra("isReminder", false);
        expectedWord = intent.getStringExtra("expectedWord");
        gesture = intent.getStringExtra("gesture");
        currentIdx = intent.getIntExtra("idx", 0);
        level = intent.getStringExtra("level");

        String packageName = getPackageName();
        int resId = getResources().getIdentifier(gesture, "string", packageName);
        String translatedGesture = getString(resId);

        remainCounterView = (TextView) findViewById(R.id.remain_counter);
        remainCounterView.setText(String.valueOf(WIN));

        counterView = (TextView) findViewById(R.id.counter);
        counterView.setText(String.valueOf(0));


        setExampleImage(translatedGesture);
    }

    private void setExampleImage(String gesture) {
        Resources resources = getResources();
        final int resourceId = resources.getIdentifier(gesture, "drawable", getPackageName());

        ImageView gestureExample = findViewById(R.id.gesture_example_img);
        gestureExample.setImageDrawable(getDrawable(resourceId));
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        if (counter < WIN) {

                            LOGGER.i("Running detection on image " + currTimestamp);
                            final long startTime = SystemClock.uptimeMillis();
                            final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
                            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                            final Canvas canvas = new Canvas(cropCopyBitmap);
                            final Paint paint = new Paint();
                            paint.setColor(Color.RED);
                            paint.setStyle(Style.STROKE);
                            paint.setStrokeWidth(2.0f);

                            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                            switch (MODE) {
                                case TF_OD_API:
                                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                    break;
                            }

                            final List<Detector.Recognition> mappedRecognitions =
                                    new ArrayList<Detector.Recognition>();

                            for (final Detector.Recognition result : results) {
                                final RectF location = result.getLocation();
                                if (location != null && result.getConfidence() >= minimumConfidence) {
                                    canvas.drawRect(location, paint);

                                    cropToFrameTransform.mapRect(location);

                                    if (result.getConfidence() >= minimumConfidence) {
                                        currentList.add(result.getTitle().replaceAll("(\\r|\\n)", ""));
                                    }

                                    if (currentList.size() == 3) {
                                        allEquals = true;

                                        for (int i = 1; i < currentList.size(); i++) {
                                            if (!currentList.get(i).equals(currentList.get(i - 1))) {
                                                allEquals = false;
                                            }
                                        }


                                        if (allEquals && currentList.get(1).equals(gesture.toUpperCase())) {
                                            readedTextArray.add(currentList.get(1));

                                            runOnUiThread(
                                                    () -> {
                                                        textValue = (TextView) findViewById(R.id.textview2);
                                                        textValue.setTextColor(getColor(R.color.green));
                                                        textValue.setText(getText(R.string.correct));
                                                        textValue.setBackground(getDrawable(R.drawable.btn_green));

                                                        counterView = (TextView) findViewById(R.id.counter);
                                                        counter = counter + 1;
                                                        counterView.setText(String.valueOf(counter));
                                                        remainCounterView = (TextView) findViewById(R.id.remain_counter);
                                                        remainCounterView.setText(String.valueOf(WIN - counter));
                                                    });

                                            if (counter == WIN - 1) {

                                                LayoutInflater inflater = (LayoutInflater)
                                                        getSystemService(LAYOUT_INFLATER_SERVICE);
                                                View popupView = inflater.inflate(R.layout.popup_next, null);

                                                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                                                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                                                boolean focusable = true; // lets taps outside the popup also dismiss it
                                                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

                                                popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);

                                                Button repeatBtn = popupView.findViewById(R.id.repeat);
                                                repeatBtn.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent resultIntent = new Intent(getApplicationContext(), LearnGestureActivity.class);
                                                        resultIntent.putExtra("expectedWord", expectedWord);
                                                        resultIntent.putExtra("gesture", gesture.toUpperCase());
                                                        resultIntent.putExtra("idx", currentIdx);
                                                        resultIntent.putExtra("isReminder", isReminder);
                                                        resultIntent.putExtra("level", level);
                                                        startActivity(resultIntent);
                                                        popupWindow.dismiss();
                                                    }
                                                });


                                                Button nextBtn = popupView.findViewById(R.id.next);
                                                if (isReminder) {
                                                    nextBtn.setText("Return!");
                                                }
                                                nextBtn.setOnClickListener(new View.OnClickListener() {

                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent resultIntent = new Intent();

                                                        if (isReminder) {
                                                            resultIntent = new Intent(getApplicationContext(), LearnWordActivity.class);
                                                            resultIntent.putExtra("level", level);
                                                            resultIntent.putExtra("expectedWord", expectedWord);
                                                            resultIntent.putExtra("idx", currentIdx);
                                                        } else {

                                                            if (currentIdx + 1 < expectedWord.length()) {
                                                                resultIntent = new Intent(getApplicationContext(), LearnGestureActivity.class);
                                                                resultIntent.putExtra("expectedWord", expectedWord);
                                                                resultIntent.putExtra("level", level);

                                                                resultIntent.putExtra("gesture", String.valueOf(expectedWord.charAt(currentIdx + 1)).toUpperCase());
                                                                resultIntent.putExtra("idx", currentIdx + 1);
                                                            }

                                                            if (currentIdx + 1 == expectedWord.length()) {
                                                                resultIntent = new Intent(getApplicationContext(), LearnWordActivity.class);
                                                                resultIntent.putExtra("level", level);
                                                                resultIntent.putExtra("expectedWord", expectedWord);
                                                            }
                                                        }

                                                        startActivity(resultIntent);
                                                        popupWindow.dismiss();
                                                    }

                                                });

                                                popupView.setOnTouchListener(new View.OnTouchListener() {
                                                    @Override
                                                    public boolean onTouch(View v, MotionEvent event) {
                                                        popupWindow.dismiss();
                                                        return true;
                                                    }
                                                });
                                            }
                                        } else {
                                            runOnUiThread(
                                                    () -> {
                                                        textValue = (TextView) findViewById(R.id.textview2);
                                                        textValue.setTextColor(getColor(R.color.yellow));
                                                        textValue.setText(getText(R.string.try_again));
                                                        textValue.setBackground(getDrawable(R.drawable.btn_yellow));
                                                    });
                                        }

                                        currentList = new ArrayList<String>();
                                    }

                                    result.setLocation(location);
                                    mappedRecognitions.add(result);
                                }
                            }

                            tracker.trackResults(mappedRecognitions, currTimestamp);
                            trackingOverlay.postInvalidate();

                            computingDetection = false;

                        }
                    }
                });
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            this,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing Detector!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(
                () -> {
                    try {
                        detector.setUseNNAPI(isChecked);
                    } catch (UnsupportedOperationException e) {
                        LOGGER.e(e, "Failed to set \"Use NNAPI\".");
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                });
    }
}
