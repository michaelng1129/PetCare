package com.eee3457.petcare.mainactivity.home;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.eee3457.petcare.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClipsScreen extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String petId;
    private TextView petName, petDetails;
    private LinearLayout videosContainer;
    private MaterialButton recordVideoButton;
    private ListenerRegistration petListener;
    private ListenerRegistration videosListener;
    private List<VideoView> activeVideoViews = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_home_clips_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        petName = view.findViewById(R.id.pet_name);
        petDetails = view.findViewById(R.id.pet_details);
        videosContainer = view.findViewById(R.id.videos_container);
        recordVideoButton = view.findViewById(R.id.record_video_button);

        // Set record button click listener (placeholder for recording)
        recordVideoButton.setOnClickListener(v -> {
            //Toast.makeText(requireContext(), "Video recording feature not implemented yet", Toast.LENGTH_SHORT).show();
            // Future: Implement video recording and upload to Firebase Storage
        });

        // Load pet data
        loadPetData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listeners and video views
        if (petListener != null) {
            petListener.remove();
        }
        if (videosListener != null) {
            videosListener.remove();
        }
        for (VideoView videoView : activeVideoViews) {
            if (videoView != null) {
                videoView.stopPlayback();
            }
        }
        activeVideoViews.clear();
    }

    private void loadPetData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        petListener = db.collection("users").document(userId).collection("pets").limit(1)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load pet data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        if (isAdded()) {
                            View petProfileCard = getView().findViewById(R.id.pet_profile_card);
                            if (petProfileCard != null) {
                                petProfileCard.setVisibility(View.GONE);
                            }
                        }
                        petId = null;
                    } else {
                        DocumentSnapshot petDoc = querySnapshot.getDocuments().get(0);
                        String name = petDoc.getString("name");
                        String birthdate = petDoc.getString("birthdate");
                        petId = petDoc.getId();

                        if (isAdded()) {
                            petName.setText(name != null ? name : "Unknown");
                            double age = calculateAge(birthdate);
                            String ageText = age < 1.0 ? String.format(Locale.US, "%.1f years old", age) : String.format(Locale.US, "%.0f years old", age);
                            petDetails.setText(String.format(ageText));
                        }

                        // Check and upload fake videos, then load videos
                        checkAndUploadFakeVideos(userId, petId);
                        loadVideos(userId, petId);
                    }
                });
    }

    private void checkAndUploadFakeVideos(String userId, String petId) {
        db.collection("users").document(userId).collection("pets").document(petId).collection("videos")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No videos found, upload fake data
                        List<Map<String, Object>> fakeVideos = generateFakeVideos();
                        for (Map<String, Object> video : fakeVideos) {
                            db.collection("users").document(userId).collection("pets").document(petId).collection("videos")
                                    .add(video)
                                    .addOnSuccessListener(docRef -> {
                                        Log.d(TAG, "Fake video added with ID: " + docRef.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to add fake video: " + e.getMessage(), e);
                                    });
                        }
                    } else {
                        Log.d(TAG, "Videos already exist, skipping fake data upload");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check videos: " + e.getMessage(), e);
                });
    }

    private List<Map<String, Object>> generateFakeVideos() {
        List<Map<String, Object>> videos = new ArrayList<>();
        String[] titles = {"Playtime at Park", "Chasing Tail"};
        String[] dates = {"2025-04-01", "2025-04-02"};
        String sampleVideoUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";

        for (int i = 0; i < 2; i++) {
            Map<String, Object> video = new HashMap<>();
            video.put("title", titles[i]);
            video.put("date", dates[i]);
            video.put("videoUrl", sampleVideoUrl);
            videos.add(video);
        }
        return videos;
    }

    private void loadVideos(String userId, String petId) {
        if (petId == null) {
            return;
        }

        videosListener = db.collection("users").document(userId).collection("pets").document(petId).collection("videos")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load videos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "Failed to load videos: " + e.getMessage(), e);
                        return;
                    }

                    if (isAdded()) {
                        videosContainer.removeAllViews();
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String title = doc.getString("title");
                        String date = doc.getString("date");
                        String videoUrl = doc.getString("videoUrl");

                        if (isAdded()) {
                            View videoCard = LayoutInflater.from(getContext()).inflate(R.layout.fragment_pet_video_card, videosContainer, false);
                            TextView videoTitle = videoCard.findViewById(R.id.video_title);
                            TextView videoDate = videoCard.findViewById(R.id.video_date);
                            ImageView playIcon = videoCard.findViewById(R.id.play_icon);

                            videoTitle.setText(title != null ? title : "Untitled Video");
                            videoDate.setText(date != null ? date : "Unknown Date");

                            // Set click listener to play video
                            videoCard.setOnClickListener(v -> showVideoPlayerDialog(videoUrl));

                            videosContainer.addView(videoCard);
                        }
                    }
                });
    }

    private void showVideoPlayerDialog(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Invalid video URL", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Stop any currently playing videos
        for (VideoView videoView : activeVideoViews) {
            if (videoView != null) {
                videoView.stopPlayback();
            }
        }
        activeVideoViews.clear();

        // Create dialog
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_video_player);

        VideoView videoView = dialog.findViewById(R.id.video_view);
        activeVideoViews.add(videoView);

        // Set up MediaController
        android.widget.MediaController mediaController = new android.widget.MediaController(requireContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // Set video URI
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.setOnPreparedListener(mp -> {
            mp.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show();
            }
            Log.e(TAG, "VideoView error: what=" + what + ", extra=" + extra);
            dialog.dismiss();
            return true;
        });
        videoView.setOnCompletionListener(mp -> {
            dialog.dismiss();
        });

        // Set close button listener
        dialog.findViewById(R.id.close_button).setOnClickListener(v -> {
            videoView.stopPlayback();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            videoView.stopPlayback();
            activeVideoViews.remove(videoView);
        });

        dialog.show();
    }

    private double calculateAge(String birthdate) {
        if (birthdate == null) {
            return 0.0;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date birthDate = sdf.parse(birthdate);
            Calendar currentCal = Calendar.getInstance();
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            long diffInMillis = currentCal.getTimeInMillis() - birthCal.getTimeInMillis();
            if (diffInMillis < 0) {
                return 0.0;
            }
            double daysPerYear = 365.25;
            double years = diffInMillis / (1000.0 * 60 * 60 * 24 * daysPerYear);
            return years >= 0 ? years : 0.0;
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Invalid birthdate format: " + birthdate, Toast.LENGTH_SHORT).show();
            }
            return 0.0;
        }
    }
}