package com.example.st_meet;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.st_meet.databinding.ActivityConferenceBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConferenceActivity extends AppCompatActivity {

    private ActivityConferenceBinding binding;
    private String meetingId;
    private String userName;
    private boolean isCaller;

    private DatabaseReference signalingRef;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private EglBase rootEglBase;

    private boolean isMicOn = true;
    private boolean isCameraOn = true;
    private boolean hasRefreshedMic = false;
    private boolean isListening = false;
    private boolean isRecordingNotes = false;
    private boolean isPeerConnected = false;

    private List<IceCandidate> pendingIceCandidates = new ArrayList<>();

    // Chat
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;

    // Chatbot
    private List<ChatbotMessage> chatbotMessages = new ArrayList<>();
    private ChatbotAdapter chatbotAdapter;
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;

    // AI Summary Constants
    private static final String API_URL = "https://api.apyhub.com/ai/summarize-text";
    private static final String APY_TOKEN = BuildConfig.APY_TOKEN;
    private final OkHttpClient client = new OkHttpClient();
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        meetingId = getIntent().getStringExtra("meeting_id");
        userName = getIntent().getStringExtra("user_name");
        isCaller = getIntent().getBooleanExtra("is_caller", false);

        if (meetingId == null) {
            Toast.makeText(this, "Invalid Meeting ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        signalingRef = FirebaseDatabase.getInstance().getReference("meetings").child(meetingId);

        // Role Negotiation: Ensure one participant is always the Caller
        signalingRef.child("callerName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() && !isCaller) {
                    isCaller = true;
                    Log.d("ConferenceActivity", "No caller found for ID: " + meetingId + ", promoting self to Caller");
                }
                runOnUiThread(() -> continueInitialization());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> continueInitialization());
            }
        });
    }

    private void continueInitialization() {
        if (isCaller) {
            Log.d("ConferenceActivity", "Role: Caller. Clearing signaling data.");
            signalingRef.child("offer").removeValue();
            signalingRef.child("answer").removeValue();
            signalingRef.child("callerCandidates").removeValue();
            signalingRef.child("joinerCandidates").removeValue();
            signalingRef.child("callerName").setValue(userName);
        } else {
            Log.d("ConferenceActivity", "Role: Joiner.");
            signalingRef.child("joinerName").setValue(userName);
        }

        // Listen for the other participant
        String otherNameNode = isCaller ? "joinerName" : "callerName";
        signalingRef.child(otherNameNode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String otherName = snapshot.getValue(String.class);
                    runOnUiThread(() -> {
                        binding.textRemoteUserName.setText(getString(R.string.participant, otherName));
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        binding.textMeetingTitle.setText(getString(R.string.meeting_id_label, meetingId));

        listenForReactions();
        listenForMessages();
        // Removed listenForTranscript() to ensure only local speech is captured for AI notes.

        if (checkPermissions()) {
            initWebRTC();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
        }

        setupClicks();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void startAudioPipeline() {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(false);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (localAudioTrack != null) {
                    localAudioTrack.setEnabled(isMicOn);
                    hasRefreshedMic = true;
                    Log.d("WebRTC", "startAudioPipeline: Mic refreshed to share stream");
                }
            }, 500);
        }
    }

    private void enableWebRTCAudio() {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(isMicOn);
            Log.d("WebRTC", "enableWebRTCAudio: Audio track enabled");
        }
    }

    private JavaAudioDeviceModule audioDeviceModule;

    private void initWebRTC() {
        if (factory != null) return;
        rootEglBase = EglBase.create();

        binding.localView.init(rootEglBase.getEglBaseContext(), null);
        binding.localView.setMirror(true);
        binding.localView.setEnableHardwareScaler(true);
        binding.localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        binding.remoteView.init(rootEglBase.getEglBaseContext(), null);
        binding.remoteView.setMirror(false);
        binding.remoteView.setEnableHardwareScaler(true);
        binding.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        // Build the audio module
        audioDeviceModule = JavaAudioDeviceModule.builder(this)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .setAudioSource(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .createAudioDeviceModule();

        PeerConnectionFactory.Options factoryOptions = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setAudioDeviceModule(audioDeviceModule)
                .setOptions(factoryOptions)
                .createPeerConnectionFactory();

        videoCapturer = createVideoCapturer();
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);
        localVideoTrack = factory.createVideoTrack("101", videoSource);
        localVideoTrack.setEnabled(isCameraOn);
        localVideoTrack.addSink(binding.localView);

        // Pre-create Audio Track so signaling works, but keep it muted at the HW level
        MediaConstraints audioConstraints = new MediaConstraints();
        AudioSource audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("102", audioSource);
        localAudioTrack.setEnabled(true);

        initPeerConnection();
    }

    private void initPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d("WebRTC", "onIceConnectionChange: " + iceConnectionState);
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    isPeerConnected = true;
                    runOnUiThread(() -> {
                        Toast.makeText(ConferenceActivity.this, "Connected to participant", Toast.LENGTH_SHORT).show();
                    });
                } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                           iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                    isPeerConnected = false;
                    runOnUiThread(() -> {
                        Toast.makeText(ConferenceActivity.this, "Participant disconnected", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                String node = isCaller ? "callerCandidates" : "joinerCandidates";
                java.util.Map<String, Object> candidate = new java.util.HashMap<>();
                candidate.put("sdpMid", iceCandidate.sdpMid);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                candidate.put("sdp", iceCandidate.sdp);
                signalingRef.child(node).push().setValue(candidate);
                Log.d("WebRTC", "Sent local ICE candidate to " + node);
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
            @Override
            public void onAddStream(MediaStream mediaStream) {
                if (!mediaStream.videoTracks.isEmpty()) {
                    Log.d("WebRTC", "onAddStream: Remote video track received");
                    runOnUiThread(() -> {
                        mediaStream.videoTracks.get(0).addSink(binding.remoteView);
                    });
                }
            }
            @Override public void onRemoveStream(MediaStream mediaStream) {}
            @Override public void onDataChannel(org.webrtc.DataChannel dataChannel) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(org.webrtc.RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                if (rtpReceiver.track() instanceof VideoTrack) {
                    Log.d("WebRTC", "onAddTrack: Remote video track received");
                    runOnUiThread(() -> {
                        ((VideoTrack) rtpReceiver.track()).addSink(binding.remoteView);
                    });
                }
                // Remote audio joined
                if (rtpReceiver.track() != null && "audio".equals(rtpReceiver.track().kind()) && !hasRefreshedMic) {
                    hasRefreshedMic = true;
                    Log.d("WebRTC", "Remote audio track joined");
                }
            }
        });

        if (localVideoTrack != null) peerConnection.addTrack(localVideoTrack, java.util.Collections.singletonList("ARDAMS"));
        if (localAudioTrack != null) peerConnection.addTrack(localAudioTrack, java.util.Collections.singletonList("ARDAMS"));

        if (isCaller) {
            createOffer();
        } else {
            listenForOffer();
        }
        listenForIceCandidates();
    }

    private void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription sd) {}
                    @Override public void onSetSuccess() {
                        Log.d("WebRTC", "Local description set (Offer), sending to Firebase");
                        signalingRef.child("offer").setValue(sessionDescription.description);
                    }
                    @Override public void onCreateFailure(String s) {}
                    @Override public void onSetFailure(String s) { Log.e("WebRTC", "setLocalDescription Failure: " + s); }
                }, sessionDescription);
                listenForAnswer();
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e("WebRTC", "createOffer Failure: " + s); }
            @Override public void onSetFailure(String s) {}
        }, constraints);
    }

    private void listenForOffer() {
        signalingRef.child("offer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !isCaller) {
                    String sdp = snapshot.getValue(String.class);
                    Log.d("WebRTC", "Offer received, setting remote description");
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                        @Override
                        public void onSetSuccess() { 
                            Log.d("WebRTC", "Remote description set (Offer), draining candidates and creating answer");
                            runOnUiThread(() -> {
                                drainIceCandidates();
                                createAnswer();
                            });
                        }
                        @Override public void onCreateFailure(String s) { Log.e("WebRTC", "setRemoteDescription Failure: " + s); }
                        @Override public void onSetFailure(String s) { Log.e("WebRTC", "setRemoteDescription Failure: " + s); }
                    }, new SessionDescription(SessionDescription.Type.OFFER, sdp));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription s) {}
                    @Override public void onSetSuccess() {
                        Log.d("WebRTC", "Local description set (Answer), sending to Firebase");
                        signalingRef.child("answer").setValue(sessionDescription.description);
                    }
                    @Override public void onCreateFailure(String s) {}
                    @Override public void onSetFailure(String s) { Log.e("WebRTC", "setLocalDescription (Answer) Failure: " + s); }
                }, sessionDescription);
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e("WebRTC", "createAnswer Failure: " + s); }
            @Override public void onSetFailure(String s) {}
        }, constraints);
    }

    private void listenForAnswer() {
        signalingRef.child("answer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isCaller) {
                    String sdp = snapshot.getValue(String.class);
                    Log.d("WebRTC", "Answer received, setting remote description");
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                        @Override
                        public void onSetSuccess() { 
                            Log.d("WebRTC", "Remote description set (Answer), draining candidates");
                            runOnUiThread(ConferenceActivity.this::drainIceCandidates);
                        }
                        @Override public void onCreateFailure(String s) { Log.e("WebRTC", "setRemoteDescription Failure (Answer): " + s); }
                        @Override public void onSetFailure(String s) { Log.e("WebRTC", "setRemoteDescription Failure (Answer): " + s); }
                    }, new SessionDescription(SessionDescription.Type.ANSWER, sdp));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForIceCandidates() {
        String listenNode = isCaller ? "joinerCandidates" : "callerCandidates";
        signalingRef.child(listenNode).addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    try {
                        String sdpMid = snapshot.child("sdpMid").getValue(String.class);
                        Object sdpMLineIndexObj = snapshot.child("sdpMLineIndex").getValue();
                        int sdpMLineIndex = 0;
                        if (sdpMLineIndexObj instanceof Long) {
                            sdpMLineIndex = ((Long) sdpMLineIndexObj).intValue();
                        } else if (sdpMLineIndexObj instanceof Integer) {
                            sdpMLineIndex = (Integer) sdpMLineIndexObj;
                        } else if (sdpMLineIndexObj instanceof Double) {
                            sdpMLineIndex = ((Double) sdpMLineIndexObj).intValue();
                        }
                        
                        String sdp = snapshot.child("sdp").getValue(String.class);
                        if (sdpMid != null && sdp != null) {
                            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                            if (peerConnection != null && peerConnection.getRemoteDescription() != null) {
                                peerConnection.addIceCandidate(candidate);
                                Log.d("WebRTC", "Added remote ICE candidate from " + listenNode);
                            } else {
                                pendingIceCandidates.add(candidate);
                                Log.d("WebRTC", "Stashed remote ICE candidate from " + listenNode);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("WebRTC", "Error parsing ICE candidate: " + e.getMessage());
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void drainIceCandidates() {
        if (peerConnection == null) return;
        for (IceCandidate candidate : pendingIceCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        pendingIceCandidates.clear();
    }

    private VideoCapturer createVideoCapturer() {
        CameraEnumerator enumerator = new Camera1Enumerator(false);
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        return enumerator.createCapturer(deviceNames[0], null);
    }

    private void setupClicks() {
        binding.buttonMic.setOnClickListener(v -> {
            isMicOn = !isMicOn;
            localAudioTrack.setEnabled(isMicOn);
            binding.buttonMic.setImageResource(isMicOn ? R.drawable.ic_mic : R.drawable.ic_mic_off);
        });

        binding.buttonCamera.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            localVideoTrack.setEnabled(isCameraOn);
            binding.buttonCamera.setImageResource(isCameraOn ? R.drawable.ic_videocam : R.drawable.ic_videocam_off);
        });

        binding.buttonSwitchCamera.setOnClickListener(v -> {
            if (videoCapturer instanceof CameraVideoCapturer) {
                ((CameraVideoCapturer) videoCapturer).switchCamera(null);
            }
        });

        binding.buttonEndCall.setOnClickListener(v -> {
            // FETCH ALL MESSAGES FROM SQLITE FOR SUMMARY
            List<Message> messages = dbHelper.getMessages(meetingId);
            StringBuilder fullChatText = new StringBuilder();
            for (Message msg : messages) {
                fullChatText.append(msg.getUserName())
                        .append(": ")
                        .append(msg.getMessageText())
                        .append(". ");
            }
            
            String finalText = fullChatText.toString().trim();
            Log.d("ConferenceActivity", "Ending call. Chat transcript length: " + finalText.length());
            generateAiSummary(finalText);
        });

        binding.buttonAiSummary.setOnClickListener(v -> showChatbotSheet());

        binding.buttonChat.setOnClickListener(v -> showChatSheet());
        binding.buttonParticipants.setOnClickListener(v -> {
            Toast.makeText(this, "Participant: " + (isCaller ? "You & Joiner" : "You & Caller"), Toast.LENGTH_SHORT).show();
        });
        binding.buttonShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Join my ST Meet meeting\nID: " + meetingId);
            startActivity(Intent.createChooser(intent, "Share Meeting ID"));
        });
        binding.btnReactThumb.setOnClickListener(v -> sendReaction("👍"));
        binding.btnReactHeart.setOnClickListener(v -> sendReaction("❤️"));
        binding.btnReactParty.setOnClickListener(v -> sendReaction("🎉"));
        binding.btnReactClap.setOnClickListener(v -> sendReaction("👏"));
    }

    private void showChatSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_chat_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView recyclerChat = sheetView.findViewById(R.id.recyclerChat);
        EditText editChatMessage = sheetView.findViewById(R.id.editChatMessage);
        View buttonSendMessage = sheetView.findViewById(R.id.buttonSendMessage);

        chatAdapter = new ChatAdapter(chatMessages);
        recyclerChat.setAdapter(chatAdapter);
        recyclerChat.scrollToPosition(chatMessages.size() - 1);

        buttonSendMessage.setOnClickListener(v -> {
            String messageText = editChatMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                ChatMessage message = new ChatMessage(userName, messageText, System.currentTimeMillis());
                signalingRef.child("messages").push().setValue(message);
                
                // Save to local SQLite immediately for analytics
                dbHelper.insertMessage(meetingId, userName, messageText, 
                    new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
                
                editChatMessage.setText("");
            }
        });

        bottomSheetDialog.show();
    }

    private void listenForMessages() {
        signalingRef.child("messages").addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    // Avoid duplicate local saves for own messages if handled in click listener, 
                    // or just rely on onChildAdded for everything. 
                    // To be safe and simple, let's check if it's from remote or if we want to rely on Firebase sync.
                    
                    chatMessages.add(message);
                    if (chatAdapter != null) chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    
                    if (!message.getSenderName().equals(userName)) {
                        Toast.makeText(ConferenceActivity.this, message.getSenderName() + ": " + message.getMessage(), Toast.LENGTH_SHORT).show();
                        // Save remote messages to local SQLite
                        dbHelper.insertMessage(meetingId, message.getSenderName(), message.getMessage(), 
                            new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(message.getTimestamp())));
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendReaction(String emoji) {
        signalingRef.child("reactions").push().setValue(emoji);
    }

    private void listenForReactions() {
        signalingRef.child("reactions").addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    String emoji = snapshot.getValue(String.class);
                    if (emoji != null) runOnUiThread(() -> showReactionAnimation(emoji));
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showChatbotSheet() {
        BottomSheetDialog chatbotSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_chatbot_sheet, null);
        chatbotSheet.setContentView(view);

        RecyclerView rvChatbot = view.findViewById(R.id.rvChatbot);
        EditText etInput = view.findViewById(R.id.etChatbotInput);
        View btnSend = view.findViewById(R.id.btnChatbotSend);
        View pbChatbot = view.findViewById(R.id.pbChatbot);

        rvChatbot.setLayoutManager(new LinearLayoutManager(this));
        chatbotAdapter = new ChatbotAdapter(chatbotMessages);
        rvChatbot.setAdapter(chatbotAdapter);

        btnSend.setOnClickListener(v -> {
            String query = etInput.getText().toString().trim();
            if (!query.isEmpty()) {
                chatbotMessages.add(new ChatbotMessage(query, true));
                chatbotAdapter.notifyItemInserted(chatbotMessages.size() - 1);
                rvChatbot.scrollToPosition(chatbotMessages.size() - 1);
                etInput.setText("");
                pbChatbot.setVisibility(View.VISIBLE);
                callGroqApi(query, pbChatbot, rvChatbot);
            }
        });

        chatbotSheet.show();
    }

    private void callGroqApi(String query, View pb, RecyclerView rv) {
        // Collect meeting context from SQLite messages
        List<Message> messages = dbHelper.getMessages(meetingId);
        StringBuilder context = new StringBuilder();
        for (Message msg : messages) {
            context.append(msg.getUserName()).append(": ").append(msg.getMessageText()).append("\n");
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "llama-3.3-70b-versatile");
            org.json.JSONArray messagesArray = new org.json.JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a meeting assistant. Use the following chat transcript to answer the user's question accurately.\n\nTranscript:\n" + context.toString());
            messagesArray.put(systemMsg);

            // Add previous chatbot conversation history for multi-turn
            for (ChatbotMessage msg : chatbotMessages) {
                JSONObject m = new JSONObject();
                m.put("role", msg.isUser() ? "user" : "assistant");
                m.put("content", msg.getText());
                messagesArray.put(m);
            }

            jsonBody.put("messages", messagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    chatbotMessages.add(new ChatbotMessage("Error: Could not reach AI assistant.", false));
                    chatbotAdapter.notifyItemInserted(chatbotMessages.size() - 1);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.has("choices")) {
                            String aiResponse = jsonResponse.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            chatbotMessages.add(new ChatbotMessage(aiResponse, false));
                        } else if (jsonResponse.has("error")) {
                            String errorMsg = jsonResponse.getJSONObject("error").getString("message");
                            chatbotMessages.add(new ChatbotMessage("AI Error: " + errorMsg, false));
                        } else {
                            chatbotMessages.add(new ChatbotMessage("Unexpected AI response format.", false));
                        }
                        chatbotAdapter.notifyItemInserted(chatbotMessages.size() - 1);
                        rv.scrollToPosition(chatbotMessages.size() - 1);
                    } catch (JSONException e) {
                        chatbotMessages.add(new ChatbotMessage("Error parsing AI response.", false));
                        chatbotAdapter.notifyItemInserted(chatbotMessages.size() - 1);
                        Log.e("GroqAPI", "Parse error: " + responseBody, e);
                    }
                });
            }
        });
    }

    private void showAiSummarySheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_ai_summary_sheet, null);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void showReactionAnimation(String emoji) {
        TextView reactionText = new TextView(this);
        reactionText.setText(emoji);
        reactionText.setTextSize(40);
        reactionText.setX(new Random().nextInt(Math.max(1, binding.reactionContainer.getWidth() - 100)));
        reactionText.setY(binding.reactionContainer.getHeight());
        binding.reactionContainer.addView(reactionText);
        reactionText.animate()
                .translationYBy(-800)
                .alpha(0f)
                .setDuration(3000)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.reactionContainer.removeView(reactionText);
                    }
                }).start();
    }


    private void generateAiSummary(String textToSummarize) {
        if (textToSummarize.trim().isEmpty()) {
            saveMeetingToHistory("No chat messages captured during meeting.", "");
            finish();
            return;
        }
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("text", textToSummarize);
            jsonBody.put("summary_length", "medium");
        } catch (JSONException e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("apy-token", APY_TOKEN)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    saveMeetingToHistory("Summary not available (Network error).", textToSummarize);
                    finish();
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                Log.d("ConferenceActivity", "AI Response: " + responseData);
                runOnUiThread(() -> {
                    String summary = "Summary not available";
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            // ApyHub sometimes returns 'data' or 'summary' depending on the specific AI endpoint version
                            if (jsonResponse.has("data")) {
                                summary = jsonResponse.getString("data");
                            } else if (jsonResponse.has("summary")) {
                                summary = jsonResponse.getString("summary");
                            } else {
                                summary = "Summary generated (check logs).";
                            }
                        } catch (JSONException e) { 
                            Log.e("ConferenceActivity", "AI Response Parse Error: " + e.getMessage());
                            summary = "Summary could not be parsed.";
                        }
                    } else {
                        Log.e("ConferenceActivity", "AI Request Unsuccessful. Code: " + response.code() + " Body: " + responseData);
                        summary = "AI Engine error (Code: " + response.code() + ")";
                    }
                    saveMeetingToHistory(summary, textToSummarize);
                    finish();
                });
            }
        });
    }

    private void saveMeetingToHistory(String summary, String fullText) {
        String currentDateTime = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
        dbHelper.insertMeeting(meetingId, currentDateTime, summary, fullText);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initWebRTC();
            } else {
                Toast.makeText(this, "Camera and Microphone permissions are required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
        }
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        if (rootEglBase != null) rootEglBase.release();
        binding.localView.release();
        binding.remoteView.release();
    }
}
