package com.example.timer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ToggleButton;


public class SettingsFragment extends Fragment {
    private View root;
    private Activity activity = getActivity();

    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root =inflater.inflate(R.layout.fragment_settings, container, false);
        EditText edit = root.findViewById(R.id.notification_time);
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String notiTime = mSharedPreferences.getString(getString(R.string.notification_time)+"Timer",edit.getText().toString());
        edit.setText(notiTime);
        final SeekBar seek = root.findViewById(R.id.powerSeek);
        String seekPower = mSharedPreferences.getString(getString(R.string.seekPow)+ "Timer",Integer.toString(seek.getProgress()));
        seek.setProgress(Integer.parseInt(seekPower));
        SwitchCompat toggle =  root.findViewById(R.id.blueToothSwitch);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("checked","hello i am checked");
                    Log.d("stillChecked","cast was successful");

                        ((MainActivity) getActivity()).connectToDevice();

                } else {
                    // The toggle is disabled
                }
            }
        });

        return root;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
       /* if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EditText editText= root.findViewById(R.id.notification_time);
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(getString(R.string.notification_time)+"Timer", editText.getText().toString());
        SeekBar seek = root.findViewById(R.id.powerSeek);
        editor.commit();
        editor.putString(getString(R.string.seekPow)+"Timer", Integer.toString(seek.getProgress()));
        editor.commit();
    }
}



