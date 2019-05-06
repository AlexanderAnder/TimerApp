package com.example.timer;


import android.os.CountDownTimer;
import android.widget.TextView;



public class Timer  {
    private CountDownTimer countShow;
    private final TextView timeText;


    public Timer(long millisInFuture, long countDownInterval, TextView text){
        timeText = text;

            countShow = new CountDownTimer (millisInFuture, countDownInterval) {

            public void onTick(long millisUntilFinished) {
               timeText.setText(setRemaining(millisUntilFinished));
            }

            public void onFinish() {
                timeText.setText(R.string.dalaran);
            }
        }.start();



    }

    public void clearTimer(boolean interuppted){
        countShow.cancel();
        if(interuppted) {
            timeText.setText(R.string.interruppt);

        } else {
            timeText.setText(R.string.dalaran);
        }
    }

    public String setRemaining(long milliseconds){
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / 60000) % 60;
        long hours = milliseconds / 3600000;
        String hour =("00" + hours).substring(Long.toString(hours).length()) ;
        String minute =("00" + minutes).substring(Long.toString(minutes).length());
        String second =("00" + seconds).substring(Long.toString(seconds).length());

        String timeRemaining = hour +":"+minute+":"+second;
        return timeRemaining ;
    }


}
