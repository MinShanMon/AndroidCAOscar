package com.example.memorygame;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    private final int GRID_COLUMNS = 3;

    private List<Image> gameImages;

    private int numberOfImagesOpened;
    private ImageView firstImage;
    private ImageView secondImage;
    private int firstImageId;
    private int secondImageId;
    private int score;
    private int maxScore;

    private boolean gameStarted;
    private boolean wrongImagePairIsStillOpen;
    private boolean flipping;
    private boolean processing;
    private boolean timerIsRunning;
    private boolean isPaused;
    private int timerSeconds;

    private List<String> strHighscores = new ArrayList<>();
  
    private MediaPlayer mediaPlayer;
    private ArrayList<MediaPlayer> mediaPlayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        strHighscores = getArray();

        findViewById(R.id.backButton).setOnClickListener(this);




        mediaPlayers = new ArrayList<>();

        RecyclerView gameRecyclerView = findViewById(R.id.gameRecyclerView);
        gameImages = GameImage.createGameImageList(this);
        GameImagesAdapter adapter = new GameImagesAdapter(gameImages, false);
        adapter.setOnItemClickListener(new GameImagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                // Start timer on first click
                if (!gameStarted) {
                    timerIsRunning = true;
                    isPaused = false;
                    gameStarted = true;
                    startTimer();
                }

                if (isPaused || flipping || processing ||
                        itemView.findViewById(R.id.gameImageView).getForeground() == null) {
                    return;
                }

                if (wrongImagePairIsStillOpen) {
                    return;
                }

                if (numberOfImagesOpened == 0) {
                    // Clicked on first image
                    firstImage = itemView.findViewById(R.id.gameImageView);
                    // Reveal image
                    flipCard(firstImage);
                    firstImageId = gameImages.get(position).getId();
                    numberOfImagesOpened = 1;
                } else if (numberOfImagesOpened == 1) {
                    // Clicked on second image
                    secondImage = itemView.findViewById(R.id.gameImageView);
                    // Reveal image
                    flipCard(secondImage);
                    secondImageId = gameImages.get(position).getId();
                    processing = true;
                    if (firstImageId == secondImageId) {
                        // Images matched
                        updateScore();

                        if (score == maxScore) {
                            // Game ended
                            stopTimer();
                            //Save high scores
                            if (strHighscores.size() < 5 || timerSeconds < convertTime(strHighscores.get(4))) {
                                // Sound effect for highscore
                                playSound(R.raw.game_highscore);
                                String score = convertTime(timerSeconds);
                                strHighscores.add(score);
                                saveArray(strHighscores);
                            } else {
                                // Sound effect for winning
                                playSound(R.raw.win_audio);
                            }

                            returnToMainActivityAfterFourSeconds();
                        } else {
                            // Game not yet end
                            // Sound effect for matching
                            playSound(R.raw.success_bell2);
                        }
                    } else {
                        // Images did not match
                        wrongImagePairIsStillOpen = true;
                        // Sound effect for wrong match
                        playSound(R.raw.failure_beep);
                        closeBothImagesAfterTwoSeconds();
                    }
                    processing = false;
                    numberOfImagesOpened = 0;
                }
            }
        });
        gameRecyclerView.setAdapter(adapter);
        gameRecyclerView.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));

        numberOfImagesOpened = 0;
        score = 0;
        maxScore = gameImages.size() / 2;
        timerIsRunning = false;
        timerSeconds = 0;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayers();
    }

    public void playSound(int soundId) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(getApplicationContext(), soundId);
            mediaPlayers.add(mediaPlayer);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mediaPlayer = null;
                }
            });
            mediaPlayer.start();
        } else {
            MediaPlayer extraMediaPlayer = MediaPlayer.create(getApplicationContext(), soundId);
            mediaPlayers.add(extraMediaPlayer);
            extraMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayers.remove(mp);
                    mp.release();
                }
            });
            extraMediaPlayer.start();
        }
    }

    public void releaseMediaPlayers() {
        for (MediaPlayer mp : mediaPlayers) {
            mp.reset();
            mp.release();
        }
    }

    public void flipCard(View v) {
        flipping = true;
        if (v.getForeground() != null) {
            v.animate().withLayer().rotationY(90).setDuration(300).withEndAction(
                    new Runnable() {
                        @Override public void run() {
                            // second quarter turn
                            v.setForeground(null);
                            v.setRotationY(-90);
                            v.animate().withLayer().rotationY(0).setDuration(300).start();
                            flipping = false;
                        }
                    }
            ).start();
        } else {
            v.animate().withLayer().rotationY(-90).setDuration(300).withEndAction(
                    new Runnable() {
                        @Override public void run() {
                            // second quarter turn
                            v.setForeground(new ColorDrawable(
                                            ContextCompat.getColor(GameActivity.this, R.color.black)));
                            v.setRotationY(90);
                            v.animate().withLayer().rotationY(0).setDuration(300).start();
                            flipping = false;
                        }
                    }
            ).start();
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.backButton) {
            finish();
        }

    }


    private void closeBothImagesAfterTwoSeconds() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                flipCard(firstImage);
                flipCard(secondImage);

                wrongImagePairIsStillOpen = false;
            }
        }, 2000);
    }

    private void updateScore() {
        score++;
        String textScore = score + "/6 matches";
        TextView textMatches = findViewById(R.id.textMatches);
        textMatches.setText(textScore);
    }

    private void startTimer() {
        final TextView timerTextView = findViewById(R.id.textTimer);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = timerSeconds / 3600;
                int minutes = (timerSeconds % 3600) / 60;
                int seconds = timerSeconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        hours, minutes, seconds);
                timerTextView.setText(time);
                if (timerIsRunning) {
                    timerSeconds++;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void stopTimer() {
        timerIsRunning = false;
    }


    private void returnToMainActivityAfterFourSeconds() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 4000);
    }

    public void saveArray(List<String> highscoreList) {
        String highscoreString = "";
        SharedPreferences sp = this.getSharedPreferences("HIGHSCORE", Activity.MODE_PRIVATE);
        SharedPreferences.Editor mEdit1 = sp.edit();
        if (highscoreList != null) {
            Collections.sort(highscoreList, (o1, o2) -> convertTime(o1) - convertTime(o2));
            if (highscoreList.size() > 5) {
                highscoreList.subList(5, highscoreList.size()).clear();
            }
            highscoreString = String.join(",", highscoreList);
            mEdit1.putString("highscoreString", highscoreString);
            mEdit1.apply();
        }
    }

    public List<String> getArray(){
        SharedPreferences sp = this.getSharedPreferences("HIGHSCORE", Activity.MODE_PRIVATE);
        String highscoreString = sp.getString("highscoreString","");
        if (highscoreString.equals("")){
            return new ArrayList<>();
        }
        else {
            ArrayList<String> highScores = new ArrayList<>(Arrays.asList(highscoreString.split(",")));
            Collections.sort(highScores, (o1, o2) -> convertTime(o1) - convertTime(o2));
            return highScores;
        }
    }

    public String convertTime(Integer intTime){
        int hours = intTime / 3600;
        int minutes = (intTime % 3600) / 60;
        int seconds = intTime % 60;
        String score = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                hours, minutes, seconds);
        return score;
    }

    public int convertTime(String strTime) {
        String[] timeUnits = strTime.split(":");
        int hours = Integer.parseInt(timeUnits[0]) * 60 * 60;
        int minutes = Integer.parseInt(timeUnits[1]) * 60;
        int seconds = Integer.parseInt(timeUnits[2]);
        return hours + minutes + seconds;
    }
}