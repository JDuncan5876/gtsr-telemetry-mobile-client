package org.gtsr.telemetry.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gtsr.telemetry.R;

public class LoggingFragment extends Fragment {

    public static LoggingFragment newInstance() {
        return new LoggingFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.logging_fragment, container, false);
    }
}
