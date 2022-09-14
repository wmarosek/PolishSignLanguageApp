package com.polishSignLanguage.detection;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.polishSignLanguage.detection.env.Logger;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;


public class LevelSelectionActivity extends AppCompatActivity {
    private static final Logger LOGGER = new Logger();

    Button btnBeginner;
    Button btnIntermediate;
    Button btnAdvanced;
    String randomString;
    String randomStringArray[] = new String[0];
    String level;
    boolean autoGenerate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.level_selection);

        Intent intent = getIntent();
        level = intent.getStringExtra("level");
        autoGenerate = intent.getBooleanExtra("autoGenerate", false);

        if (!autoGenerate) {
            btnBeginner = findViewById(R.id.beginner);
            btnIntermediate = findViewById(R.id.intermediate);
            btnAdvanced = findViewById(R.id.advanced);

            btnBeginner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), LearnGestureActivity.class);

                    randomStringArray = getRandomWordByLevel("begginer");
                    intent.putExtra("expectedWord", TextUtils.join("", randomStringArray));
                    intent.putExtra("gesture", String.valueOf(randomStringArray[0]));
                    intent.putExtra("idx", 0);
                    intent.putExtra("level", "begginer");
                    startActivity(intent);
                }
            });

            btnIntermediate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), LearnGestureActivity.class);

                    randomStringArray = getRandomWordByLevel("intermediate");
                    intent.putExtra("expectedWord", TextUtils.join("", randomStringArray));
                    intent.putExtra("gesture", String.valueOf(randomStringArray[0]));
                    intent.putExtra("idx", 0);
                    intent.putExtra("level", "intermediate");
                    startActivity(intent);
                }
            });

            btnAdvanced.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), LearnGestureActivity.class);
                    randomStringArray = getRandomWordByLevel("advanced");
                    intent.putExtra("expectedWord", TextUtils.join("", randomStringArray));
                    intent.putExtra("gesture", String.valueOf(randomStringArray[0]));
                    intent.putExtra("idx", 0);
                    intent.putExtra("level", "advanced");
                    startActivity(intent);
                }
            });

        } else {

            Intent nextIntent = new Intent(getApplicationContext(), LearnGestureActivity.class);
            randomStringArray = getRandomWordByLevel(level);
            nextIntent.putExtra("expectedWord", TextUtils.join("", randomStringArray));
            nextIntent.putExtra("level", level);
            nextIntent.putExtra("gesture", String.valueOf(randomStringArray[0]));
            nextIntent.putExtra("idx", 0);
            startActivity(nextIntent);

        }

    }

    protected String[] getRandomWordByLevel(String level) {
        String generatedString = new String();
        String[] stringArrayToRandom = new String[0];
        String[] generatedStringArray = new String[0];

        if (level.equals("begginer")) {
            stringArrayToRandom = getResources().getStringArray(R.array.beginner_letters);
            int leftLimLen = 2;
            int rightLimLen = 5;
            int stringLength = new Random().nextInt(rightLimLen - leftLimLen) + leftLimLen;
            Random randomIndex = new Random();
            for(int i = 0; i < stringLength; i++) {
                int randomLimitedInt = randomIndex.nextInt(stringArrayToRandom.length);
                if (String.valueOf(stringArrayToRandom[randomLimitedInt]).length() == 1) {
                    generatedStringArray = ArrayUtils.add(generatedStringArray, stringArrayToRandom[randomLimitedInt]);
                } else {
                    i--;
                }
            }
        }

        if (level.equals("intermediate")) {
            stringArrayToRandom = getResources().getStringArray(R.array.intermediate_letters);
            int leftLimLen = 4;
            int rightLimLen = 12;
            int stringLength = new Random().nextInt(rightLimLen - leftLimLen) + leftLimLen;
            Random randomIndex = new Random();
            for(int i = 0; i < stringLength; i++) {
                int randomLimitedInt = randomIndex.nextInt(stringArrayToRandom.length);
                generatedStringArray = ArrayUtils.add(generatedStringArray, stringArrayToRandom[randomLimitedInt]);
            }
        }

        if (level.equals("advanced")) {
            stringArrayToRandom = getResources().getStringArray(R.array.advanced_letters);
            int leftLimLen = 8;
            int rightLimLen = 20;
            int stringLength = new Random().nextInt(rightLimLen - leftLimLen) + leftLimLen;
            Random randomIndex = new Random();
            for(int i = 0; i < stringLength; i++) {
                int randomLimitedInt = randomIndex.nextInt(stringArrayToRandom.length);
                generatedStringArray = ArrayUtils.add(generatedStringArray, stringArrayToRandom[randomLimitedInt]);
            }
        }

        return generatedStringArray;
    }


}