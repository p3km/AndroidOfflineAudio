package com.example.lessonia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.net.Uri;
import android.os.Bundle;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioPlayer extends AppCompatActivity {
    Button playButton;
    SeekBar positionBar;
    TextView elapsedTimeLabel, remainingTimeLabel, titleLabel;
    MediaPlayer mediaPlayer;
    int totalTime;
    String fileName, filePath; // default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        //Init data from intent (passed on from previous activity)
        fileName = getIntent().getStringExtra("titleLabel");
        filePath = getIntent().getStringExtra("filePath");

        // Set up action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // back button

        playButton = (Button) findViewById(R.id.playButton);
        elapsedTimeLabel = (TextView) findViewById(R.id.elapsedTimeLabel);
        remainingTimeLabel = (TextView) findViewById(R.id.remainingTimeLabel);
        titleLabel = (TextView) findViewById(R.id.titleLabel);
        titleLabel.setText(fileName);

        // Initialize media player
//        if (!fileName.equals("MV_Jack_Stauber_Buttercup.mp3")){
//            mediaPlayer = MediaPlayer.create(this, R.raw.music);
//        }else{
//            //TODO try media player below when files added
//        }
        mediaPlayer = MediaPlayer.create(this, Uri.parse(filePath));

        mediaPlayer.setLooping(false);
        mediaPlayer.seekTo(0);
        mediaPlayer.setVolume(0.5f,0.5f);
        totalTime = mediaPlayer.getDuration();

        // Time tracker Bar
        positionBar = (SeekBar) findViewById(R.id.positionBar);
        positionBar.setMax(totalTime);

        positionBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int time, boolean user) {
                        if (user){
                            mediaPlayer.seekTo(time);
                            positionBar.setProgress(time);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                }
        );

        // Update image of time tracker bar
        new Thread(new Runnable() {
            @Override
            public void run() { // run once thread is started
                while(mediaPlayer != null){
                    try {
                        Message msg = new Message();
                        msg.what = mediaPlayer.getCurrentPosition(); // gets the current position in the file
                        handler.sendMessage(msg); // apply the new position

                        Thread.sleep(1000);
                    }catch (InterruptedException e){}
                }
            }
        }).start();


    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int currentPosition = msg.what;

            // updates bar
            positionBar.setProgress(currentPosition);

            // updates labels
            String elapsedTime = createTimeLabel(currentPosition);
            elapsedTimeLabel.setText(elapsedTime);

            String remainingTime = createTimeLabel(totalTime - currentPosition);
            remainingTimeLabel.setText("-"+remainingTime);

        }
    };

    private String createTimeLabel(int time){
        String timeLabel = "";
        int minutes = time/1000/60;
        int seconds = time/1000 % 60;
        timeLabel = minutes + ":";
        if (seconds < 10) timeLabel += "0";
        timeLabel += seconds;
        return timeLabel;
    }
    public void playButtonClick(View view){
        // If paused start, else stop
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
            playButton.setBackgroundResource(R.drawable.stop);
        }else{
            mediaPlayer.stop();
            playButton.setBackgroundResource(R.drawable.play);
        }
    }
}
