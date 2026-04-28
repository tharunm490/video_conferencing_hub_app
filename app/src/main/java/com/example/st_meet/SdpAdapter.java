package com.example.st_meet;

import android.util.Log;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SdpAdapter implements SdpObserver {
    @Override public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d("WebRTC", "SDP Create Success");
    }
    @Override public void onSetSuccess() {
        Log.d("WebRTC", "SDP Set Success");
    }
    @Override public void onCreateFailure(String s) {
        Log.e("WebRTC", "SDP Create Failure: " + s);
    }
    @Override public void onSetFailure(String s) {
        Log.e("WebRTC", "SDP Set Failure: " + s);
    }
}
