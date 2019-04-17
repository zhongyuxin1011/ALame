package com.zhongyuxin.alame;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zhongyuxin.alame.core.Alame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static String[] BITRATE_MPEG2_5 = {"8", "16", "24", "32", "40", "48", "56", "64"};
    private static String[] BITRATE_MPEG2 = {"8", "16", "24", "32", "40", "48", "56", "64", "80",
            "96", "112", "128", "144", "160"};
    private static String[] BITRATE_MPEG1 = {"32", "40", "48", "56", "64", "80", "96", "112",
            "128", "160", "192", "224", "256", "320"};
    private static String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission
            .WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @BindView(R.id.textView_sample)
    TextView mTextViewSample;
    @BindView(R.id.seekBar_sample)
    SeekBar mSeekBarSample;
    @BindView(R.id.radioButton_single)
    RadioButton mRadioButtonSingle;
    @BindView(R.id.radioButton_double)
    RadioButton mRadioButtonDouble;
    @BindView(R.id.radioGroup_sound)
    RadioGroup mRadioGroupSound;
    @BindView(R.id.radioButton_cbr)
    RadioButton mRadioButtonCbr;
    @BindView(R.id.radioButton_vbr)
    RadioButton mRadioButtonVbr;
    @BindView(R.id.radioButton_abr)
    RadioButton mRadioButtonAbr;
    @BindView(R.id.radioGroup_code)
    RadioGroup mRadioGroupCode;
    @BindView(R.id.textView_quality)
    TextView mTextViewQuality;
    @BindView(R.id.seekBar_quality)
    SeekBar mSeekBarQuality;
    @BindView(R.id.textView_bitrate)
    TextView mTextViewBitrate;
    @BindView(R.id.spinner_bitrate)
    Spinner mSpinnerBitrate;
    @BindView(R.id.textView_path)
    TextView mTextViewPath;
    @BindView(R.id.button_play)
    Button mButtonPlay;
    @BindView(R.id.button_pause)
    Button mButtonPause;
    @BindView(R.id.button_stop)
    Button mButtonStop;
    @BindView(R.id.progressBar_time)
    ProgressBar mProgressBar;
    @BindView(R.id.textView_version)
    TextView mTextViewVersion;
    @BindView(R.id.imageView_action)
    ImageView mImageViewAction;

    private long mHandle;
    private AudioCapture mAudioCapture;
    private byte[] mMp3Buff;
    private FileOutputStream mStream;

    private volatile boolean mProcess;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler;
    private boolean mIsPlaying;
    private Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initView();
        initEvent();
    }

    private void initView() {
        mTextViewVersion.setText(Alame.getAlame().version());
        mSpinnerBitrate.setEnabled(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_item, android.R.id.text1, BITRATE_MPEG2_5);
        mSpinnerBitrate.setAdapter(adapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        mImageViewAction.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    checkPermission(true);
                    ScaleAnimation animation = new ScaleAnimation(
                            1f, 1.5f, 1f, 1.5f, mImageViewAction.getWidth() / 2, mImageViewAction
                            .getHeight() / 2);
                    animation.setRepeatMode(Animation.REVERSE);
                    animation.setRepeatCount(Animation.INFINITE);
                    animation.setDuration(300);
                    mImageViewAction.startAnimation(animation);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_UP:
                    release();
                    mImageViewAction.clearAnimation();
                    break;
            }
            return false;
        });
        mSeekBarSample.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android
                            .R.layout
                            .simple_spinner_item, android.R.id.text1, BITRATE_MPEG2_5);
                    if (i <= 8000) {
                        seekBar.setProgress(8000);
                    } else if (i <= 16000) {
                        seekBar.setProgress(16000);
                        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout
                                .simple_spinner_item, android.R.id.text1, BITRATE_MPEG2);
                    } else if (i <= 22050) {
                        seekBar.setProgress(22050);
                        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout
                                .simple_spinner_item, android.R.id.text1, BITRATE_MPEG2);
                    } else if (i <= 32000) {
                        seekBar.setProgress(32000);
                        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout
                                .simple_spinner_item, android.R.id.text1, BITRATE_MPEG1);
                    } else {
                        seekBar.setProgress(44100);
                        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout
                                .simple_spinner_item, android.R.id.text1, BITRATE_MPEG1);
                    }
                    mSpinnerBitrate.setAdapter(adapter);
                } else {
                    mTextViewSample.setText("Sample: " + i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    mTextViewQuality.setText("Quality: " + i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mRadioGroupCode.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.radioButton_abr:
                case R.id.radioButton_vbr:
                    mSpinnerBitrate.setEnabled(false);
                    break;
                case R.id.radioButton_cbr:
                    mSpinnerBitrate.setEnabled(true);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 0) {
            checkPermission(false);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1 && permissions.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String permission : permissions) {
                sb.append(permission).append(" ");
            }
            sb.append(" - ");
            for (int grantResult : grantResults) {
                sb.append(grantResult).append(" ");
            }
            Log.e(TAG, "permissions:" + sb.toString());

            String tmp = "你拒绝了权限\"";
            boolean showFlag = false;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showFlag = true;
                    tmp += permissions[i];
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            permissions[i])) {
                        ActivityCompat.requestPermissions(this, new String[]{permissions[i]}, 1);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("请求权限：");
                        builder.setMessage(permissions[i]);
                        builder.setNegativeButton("确定", (dialog, which) -> {
                            Uri uri = Uri.parse("package:" + getPackageName());
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    uri);
                            startActivityForResult(intent, 0);
                        });
                        builder.setPositiveButton("取消", null);
                        builder.show();
                    }
                }
            }
            tmp = tmp.substring(0, tmp.length() - 1 - 1);
            tmp += "\"";
            if (showFlag) {
                Toast.makeText(this, tmp, Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkPermission(boolean action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> tmp = new ArrayList<>();
            for (String permission : PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager
                        .PERMISSION_GRANTED) {
                    tmp.add(permission);
                }
            }
            if (!tmp.isEmpty()) {
                Log.e(TAG, "permission: " + tmp.toString());
                ActivityCompat.requestPermissions(this, tmp.toArray(new String[tmp.size()]), 1);
            } else {
                if (action) {
                    press();
                }
            }
        } else {
            if (action) {
                press();
            }
        }
    }

    private void press() {
        mButtonStop.performClick();
        mButtonPlay.setEnabled(false);
        mProgressBar.setProgress(0);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/LameTest");
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            UUID uuid = UUID.randomUUID();
            String path = file.getPath() + "/" + uuid + ".mp3";
            mStream = new FileOutputStream(path, false);
            mTextViewPath.setText(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int sampleRate = mSeekBarSample.getProgress();
        int channels = mRadioButtonSingle.isChecked() ? 1 : 2;
        int bitrate = Integer.parseInt((String) mSpinnerBitrate.getSelectedItem());
        int mode = channels == 1 ? 3 : 0;
        int vbr = mRadioButtonCbr.isChecked() ? 0 : (mRadioButtonAbr.isChecked() ? 3 : 6);
        int quality = mSeekBarQuality.getProgress();
        mHandle = Alame.getAlame().createHandle(sampleRate, channels, bitrate, mode, vbr, quality);
        Log.e(TAG, "createHandle: " + mHandle);
        mProcess = true;

        mAudioCapture = new AudioCapture();
        mAudioCapture.startCapture(sampleRate, channels == 1 ? AudioFormat.CHANNEL_IN_MONO :
                AudioFormat.CHANNEL_IN_STEREO);
        mAudioCapture.setOnAudioFrameCapturedListener((minBuffSize, data, sample) -> {
            if (mMp3Buff == null) {
                mMp3Buff = new byte[(int) (1.25 * minBuffSize + 7200)];
            }
            if (!mProcess) {
                return;
            }
            int encode = Alame.getAlame().encodeMp3(mHandle, data, data.length, mMp3Buff,
                    mMp3Buff.length);
            Log.e(TAG, "encode: " + encode);
            if (encode > 0 && mStream != null) {
                try {
                    mStream.write(mMp3Buff, 0, encode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void release() {
        mButtonPlay.setEnabled(true);
        if (mAudioCapture != null) {
            mAudioCapture.stopCapture();
            mAudioCapture = null;
        }

        mProcess = false;

        if (mMp3Buff != null) {
            int flush = Alame.getAlame().flush(mHandle, mMp3Buff, mMp3Buff.length);
            Log.e(TAG, "flush: " + flush);
            if (flush > 0 && mStream != null) {
                try {
                    mStream.write(mMp3Buff, 0, flush);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            if (mStream != null) {
                mStream.close();
                mStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int destroy = Alame.getAlame().destroyHandle(mHandle);
        Log.e(TAG, "destroyHandle: " + destroy);
        mHandle = -1;

        mMp3Buff = null;
    }

    @SuppressLint("HandlerLeak")
    public void play() {
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case 1:
                            if (mMediaPlayer != null) {
                                mButtonPlay.setEnabled(false);
                                mButtonPause.setEnabled(true);
                                mButtonStop.setEnabled(true);
                                mProgressBar.setProgress(0);
                                mProgressBar.setMax(mMediaPlayer.getDuration());
                                mIsPlaying = true;
                                if (mRunnable == null && mHandler != null) {
                                    mRunnable = () -> {
                                        if (mIsPlaying && mMediaPlayer != null) {
                                            mProgressBar.setProgress(mMediaPlayer
                                                    .getCurrentPosition());
                                            if (mHandler != null) {
                                                mHandler.postDelayed(mRunnable, 1000);
                                            }
                                        }
                                    };
                                    mHandler.postDelayed(mRunnable, 1000);
                                }
                            }
                            break;
                        case 0:
                            if (mMediaPlayer != null) {
                                mButtonPlay.setEnabled(true);
                                mButtonPause.setEnabled(false);
                                mButtonStop.setEnabled(false);
                                mProgressBar.setProgress(mProgressBar.getMax());
                                if (mHandler != null) {
                                    mHandler.removeCallbacksAndMessages(null);
                                    mRunnable = null;
                                    mHandler = null;
                                }
                                mMediaPlayer.stop();
                                mMediaPlayer.release();
                                mMediaPlayer = null;
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
        }
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(this, Uri.parse(mTextViewPath.getText()
                    .toString()));
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.start();
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(1);
                }
            });
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(0);
                }
            });
        } else {
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        }
    }

    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mButtonPlay.setEnabled(true);
            mButtonPause.setEnabled(false);
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            if (mHandler != null) {
                mHandler.sendEmptyMessage(0);
            }
        }
    }

    @OnClick({R.id.button_play, R.id.button_pause, R.id.button_stop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.button_play:
                play();
                break;
            case R.id.button_pause:
                pause();
                break;
            case R.id.button_stop:
                stop();
                break;
        }
    }
}
