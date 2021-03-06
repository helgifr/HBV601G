package com.example.gudmundurorripalsson.hvaderibio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.wefika.flowlayout.FlowLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.gudmundurorripalsson.hvaderibio.R.string.leikstjorar;
import static com.example.gudmundurorripalsson.hvaderibio.R.string.leikstjori;

/**
 * Created by Helgi on 24/03/2018.
 */

/**
 * Þegar mynd er valinn af forsíðu opnast MovieFragment þar sem allar helstu upplýsingar um myndina
 * er hægt að sjá svo sem lýsingu á mynd, sýngartíma, aldurstakmark og trailer. Þegar smellt er á
 * "Rate Movie" er farið yfir á RateFragment. Þegar smellt er á "Showtimes" og sýningartími er valinn
 * er farið yfir á vefsíðu fyrir miðasölu bíóhússins sem var valið. Þegar smellt er á imdb merkið er farið á
 * imdb vefsíðu myndarinnar.
 */

public class MovieFragment extends Fragment {

    private Movie movie;
    private View mView;
    private String descr;
    private String cert;
    private JSONObject json;
    private FirebaseDatabase database;
    private String arg;
    public Score score;
    private FirebaseUser user;
    private String username;

    // youtube player to play video when new video selected
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    public static final String TAG = MovieFragment.class.getSimpleName();

    public MovieFragment() {
        // Required empty public constructor
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            Activity a = getActivity();
            if(a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        score = new Score();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_movie, container, false);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null)
            username = user.getDisplayName();

        arg = getArguments().getString("movie");
        try {
            json = new JSONObject(arg);
        } catch (JSONException e) {
            Log.e(TAG, "Exception caught: ", e);
        }

        youTubePlayerFragment = (YouTubePlayerSupportFragment) getChildFragmentManager().findFragmentById(R.id.videoView);

        if (youTubePlayerFragment == null) {
            Log.d(TAG, "oh boy");
        }

        try {
            JSONObject json = new JSONObject(arg);
            List<String> directors = new ArrayList<>();
            JSONArray directorsJSON = json.getJSONArray("directors_abridged");
            for (int i = 0; i < directorsJSON.length(); i++) {
                directors.add(directorsJSON.getJSONObject(i).getString("name"));
            }
            movie = new Movie(
                    json.getInt("id"),
                    json.getString("title"),
                    json.getJSONObject("ratings").getString("imdb"),
                    json.getString("poster"),
                    json.getString("certificateImg"),
                    json.getString("plot"),
                    directors
            );
        } catch (JSONException e) {
            Log.e(TAG, "Exception caught: ", e);
        }


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mRef = database.getReference().child("Movies").child(String.valueOf(movie.getId()));
        mRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of users in datasnapshot
                        if(dataSnapshot.getValue() != null) {
                            double rating = score.collectRating((Map<String, Object>) dataSnapshot.getValue());
                            TextView bioRating = (TextView) mView.findViewById(R.id.bioRating);
                            DecimalFormat df = new DecimalFormat("#.#");
                            bioRating.setText(df.format(rating));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error caught: " + databaseError);
                    }
                });


        // Virkar bara fyrir API sem eru 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setEnterTransition(new Fade());
        }





        updateView();
        Button rateButton = mView.findViewById(R.id.buttonRate);
        rateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(user != null){
                    int movieID = movie.getId();
                    String poster = movie.getPoster();
                    View posterView = mView.findViewById(R.id.movieImage);

                    RateFragment rateFragment = new RateFragment(movieID, username, poster);
                    Bundle bundle = new Bundle();
                    bundle.putString("movie", arg);
                    rateFragment.setArguments(bundle);

                    int FADE_DEFAULT_TIME = 250;
                    int MOVE_DEFAULT_TIME = 250;

                    // Virkar bara fyrir API sem eru 21+

                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        // 1. Exit for Previous Fragment
                        Fade exitFade = new Fade();
                        exitFade.setDuration(FADE_DEFAULT_TIME + MOVE_DEFAULT_TIME);
                        //rateFragment.setExitTransition(exitFade);

                        // 2. Shared Elements Transition
                        TransitionSet enterTransitionSet = new TransitionSet();
                        enterTransitionSet.addTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                        enterTransitionSet.setDuration(MOVE_DEFAULT_TIME);
                        enterTransitionSet.setStartDelay(FADE_DEFAULT_TIME);
                        rateFragment.setSharedElementEnterTransition(enterTransitionSet);

                        // 3. Enter Transition for New Fragment
                        Fade enterFade = new Fade();
                        enterFade.setStartDelay(FADE_DEFAULT_TIME + MOVE_DEFAULT_TIME);
                        enterFade.setDuration(FADE_DEFAULT_TIME);
                        //rateFragment.setEnterTransition(enterFade);

                    }
                    System.out.println(posterView.getTransitionName());
                    fragmentTransaction.addSharedElement(posterView, "rateTransition");
                    fragmentTransaction.replace(R.id.main_frame, rateFragment);
                    fragmentTransaction.addToBackStack(TAG);
                    fragmentTransaction.commit();

                }
                else{
                    Toast.makeText(getContext(), "Login to rate movies.",
                            Toast.LENGTH_SHORT).show();
                }

            }});



        ImageButton videoButton = mView.findViewById(R.id.playVideo);
        videoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ImageView poster = getActivity().findViewById(R.id.movieImage);
                poster.setVisibility(View.INVISIBLE);
                getActivity().findViewById(R.id.playVideo).setVisibility(View.GONE);
                Log.d(TAG, "onClick: Initializing Youtube Player.");
                youTubePlayerFragment.initialize(YoutubeConfig.getApiKey(), new YouTubePlayer.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                        Log.d(TAG, "onClick: Done initializing.");
                        try {
                            List<String> videoList = new ArrayList<>();
                            JSONArray trailers = json.getJSONArray("trailers").getJSONObject(0).getJSONArray("results");
                            if (trailers.length() == 0) {
                                throw new JSONException("No trailers");
                            }
                            for (int i = 0; i < trailers.length(); i++) {
                                String video = trailers.getJSONObject(i).getString("url");
                                videoList.add(video.substring(30, 41));
                            }
                            Log.d(TAG, " " + videoList.toString());
                            youTubePlayer.loadVideos(videoList);
                        } catch (JSONException e) {
                            Log.e(TAG, "Exception caught: ", e);
                            youTubePlayer.release();
                            Toast noVideo = Toast.makeText(getContext(), R.string.no_video_available, Toast.LENGTH_LONG);
                            noVideo.setGravity(Gravity.TOP, 0, 400);
                            noVideo.show();
                        }
                    }

                    @Override
                    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                        Log.d(TAG, "onClick: Failed to initialize.");
                    }
                });
            }
        });

        /**
         *  Sýnir Sýningartímana
         */
        Button showtimes = mView.findViewById(R.id.showtimes);
        showtimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                int margin = 20;

                View d = new View(getContext());
                d = inflater.inflate(R.layout.showtimes, null);
                ScrollView scrollView = (ScrollView) d.findViewById(R.id.scrollView);
                TableLayout mTableView = (TableLayout) d.findViewById(R.id.scheduleTable);

                try {
                    JSONArray showtimes = json.getJSONArray("showtimes");
                    for (int i = 0; i < showtimes.length(); i++) {
                        JSONObject theater = showtimes.getJSONObject(i);
                        TextView theaterName = new TextView(getContext());
                        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(margin, margin, margin, margin);
                        theaterName.setLayoutParams(layoutParams);
                        theaterName.setText(theater.getJSONObject("cinema").getString("name"));
                        theaterName.setTextColor(Color.BLACK);
                        theaterName.setGravity(Gravity.CENTER_HORIZONTAL);
                        TableRow tableRow = new TableRow(getContext());
                        tableRow.addView(theaterName);
                        FlowLayout flowLayout = new FlowLayout(getContext());
                        JSONArray schedules = theater.getJSONArray("schedule");
                        for (int j = 0; j < schedules.length(); j++) {
                            Button schedule = new Button(getContext());
                            schedule.setText(schedules.getJSONObject(j).getString("time"));
                            schedule.setTextSize(13);
                            addUrlToBtn(schedule, schedules.getJSONObject(j).getString("purchase_url"));
                            schedule.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.showtime_button_background));
                            FlowLayout.LayoutParams scheduleLayout = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            scheduleLayout.setMargins(margin/2, margin/2, margin/2, margin/2);
                            schedule.setLayoutParams(scheduleLayout);
                            flowLayout.addView(schedule);
                        }
                        TableRow.LayoutParams layoutParams1 = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams1.setMargins(margin, margin, margin, margin);
                        flowLayout.setLayoutParams(layoutParams1);
                        tableRow.addView(flowLayout);
                        tableRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        mTableView.addView(tableRow);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Exception caught: ", e);
                }

                DisplayMetrics dm = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

                int width = dm.widthPixels;
                int height = dm.heightPixels;

                final PopupWindow popupWindow = new PopupWindow(
                        scrollView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                // Virkar bara fyrir API sem eru 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.setElevation(5.0f);
                    popupWindow.setEnterTransition(new Slide());
                }

                // Set a click listener for the popup window close button
                popupWindow.setFocusable(true);
                popupWindow.setOutsideTouchable(true);
                scrollView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                popupWindow.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);

                getActivity().getWindow().getDecorView().setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false;
                    }
                });
            }
        });

        /**
         * IMDb síða myndar sett á IMDb merkið
         */
        ImageButton imdbButton = (ImageButton) mView.findViewById(R.id.imdbButton);
        try {
            String imdbUrl = json.getJSONObject("ids").getString("imdb");
            imdbUrl = "https://www.imdb.com/title/tt" + imdbUrl;
            addUrlToBtn(imdbButton, imdbUrl);
        } catch (JSONException e) {
            Log.e(TAG, "Exception caught: ", e);
        }


        return mView;
    }



    @Override
    public void onPause() {
        ImageView poster = getActivity().findViewById(R.id.movieImage);
        poster.setVisibility(View.VISIBLE);
        super.onPause();
    }





    private void updateView() {
        ImageView imageView = (ImageView) mView.findViewById(R.id.movieImage);
        TextView titleView = (TextView) mView.findViewById(R.id.movieTitle);
        TextView descrView = (TextView) mView.findViewById(R.id.movieDescr);
        TextView imdbRatingView = (TextView) mView.findViewById(R.id.imdbRating);
        ImageView certView = (ImageView) mView.findViewById(R.id.movieCertificate);
        TextView directorView = (TextView) mView.findViewById(R.id.director);
        TextView bioRatingView = (TextView) mView.findViewById(R.id.bioRating);

        Picasso.with(getContext()).load(movie.getPoster()).into(imageView);
        titleView.setText(movie.getTitle());
        descrView.setText(movie.getDescr());
        String imdb = "";
        if (!movie.getImdb().equals("null")) {
            imdb = movie.getImdb();
        }
        imdbRatingView.setText(imdb);
        //bioRatingView.setText(value);
        Picasso.with(getContext()).load(movie.getCert()).into(certView);
        List<String> directors = movie.getDirectors();
        int directorsSize = directors.size();
        if (directorsSize > 1) {
            StringBuilder directorList = new StringBuilder().append(directors.get(0));
            // String text = getString(R.string.leikstjorar, directors.get(0));
            for (int i = 1; i < directorsSize - 1; i++) {
                directorList.append(", ").append(directors.get(i));
            }
            directorList.append(" " + getString(R.string.and) + " ").append(directors.get(directorsSize - 1));
            // directorView.setText(text);
            directorView.setText(getString(leikstjorar, directorList.toString()));
        } else
            directorView.setText(getString(leikstjori, directors.get(0)));
    }

    public void addUrlToBtn(Button button, final String url) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i2);
            }
        });
    }

    public void addUrlToBtn(ImageButton button, final String url) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i2);
            }
        });
    }



}
