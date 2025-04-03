package com.example.biblequiz;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProgressBarFragment extends Fragment {
    private static final String TAG = "ProgressBarDebug";
    private ProgressBar progressBar;
    private Handler handler = new Handler(Looper.getMainLooper());

    public ProgressBarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);  // Prevent fragment from being destroyed on rotation
        Log.d(TAG, "ProgressBarFragment onCreate() called");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "ProgressBarFragment onCreateView() called");

        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_progress_bar, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "ProgressBarFragment onViewCreated() called");

        progressBar = view.findViewById(R.id.progress_bar);
        if (progressBar == null) {
            Log.e(TAG, "ProgressBar is null! Check if the layout is properly inflated.");
            return;
        }

        // Show progress bar initially
        showProgress();

        Log.d(TAG, "ProgressBar visibility after onViewCreated(): " + progressBar.getVisibility());

     /*   // Auto-hide after 2.5 seconds (2500ms)
        handler.postDelayed(() -> {
            hideProgress();
        }, 2500);
*/

    }

    public void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "ProgressBar is now VISIBLE.");
        } else {
            Log.e(TAG, "Attempted to show ProgressBar but it's NULL!");
        }
    }

    public void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            Log.d(TAG, "ProgressBar is now GONE.");
        } else {
            Log.e(TAG, "Attempted to hide ProgressBar but it's NULL!");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "ProgressBarFragment onDestroyView() called");

        // Cancel any pending delayed execution to prevent crashes
        handler.removeCallbacksAndMessages(null);
    }
}
