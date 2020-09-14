package com.nocola.cordova.plugin;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.widget.TextView;

import com.csl.cs108library4a.Cs108Library4A;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import java.io.IOException;

public class CustomMediaPlayer {
    final boolean DEBUG = false;
    Context context;
    MediaPlayer player;
    boolean starting = false;

    public static SharedObjects sharedObjects;
    private static CordovaPlugin that;
    public static Context mContext = that.cordova.getActivity();
    private static CordovaInterface layout;
    public static TextView mLogView = new TextView(layout.getContext());
    public static Cs108Library4A mCs108Library4a = new Cs108Library4A(mContext, mLogView);

    public CustomMediaPlayer(Context context, String file) {
        this.context = context;
        player = null;
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(file);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    starting = false;
                    if (DEBUG) mCs108Library4a.appendToLog("MediaPlayer is completed.");
                }
            });
        } catch (IOException e) {
            mCs108Library4a.appendToLog("mp3 setup FAIL");
        }
    }

    public void start() {
        player.start();
        if (false) starting = true;
    }

    public boolean isPlaying() {
        return (player.isPlaying() | starting);
    }

    public void pause() {
        player.pause();
    }

    void setVolume(int volume1, int volume2) {
        if (false) player.setVolume(volume1, volume2);
        else {
            AudioManager audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
            int iVolumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mCs108Library4a.appendToLog("Hello8: currentVolume = " + currentVolume);
            if (currentVolume > 0) {
                int volume12 = volume1 + volume2;
                volume12 = (volume12 * iVolumeMax) / 600;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, iVolumeMax, 0);
            }
        }
    }
}
