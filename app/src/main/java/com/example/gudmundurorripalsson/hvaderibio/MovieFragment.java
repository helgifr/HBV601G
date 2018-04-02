package com.example.gudmundurorripalsson.hvaderibio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.transition.Fade;
import android.transition.Slide;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.wefika.flowlayout.FlowLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Helgi on 24/03/2018.
 */

public class MovieFragment extends Fragment {

    private Movie movie;
    private View mView;
    private String descr;
    private String cert;
    private JSONObject json;

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

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_movie, container, false);

        String arg = getArguments().getString("movie");
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
            movie = new Movie(
                    json.getInt("id"),
                    json.getString("title"),
                    json.getJSONObject("ratings").getString("imdb"),
                    json.getString("poster"),
                    json.getString("certificateImg"),
                    json.getString("plot")
            );
        } catch (JSONException e) {
            Log.e(TAG, "Exception caught: ", e);
        }

        updateView();

        // Virkar bara fyrir API sem eru 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setEnterTransition(new Fade());
        }

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
                            for (int i = 0; i < trailers.length(); i++) {
                                String video = trailers.getJSONObject(i).getString("url");
                                videoList.add(video.substring(30, 41));
                            }
                            Log.d(TAG, " " + videoList.toString());
                            youTubePlayer.loadVideos(videoList);
                        } catch (JSONException e) {
                            Log.e(TAG, "Exception caught: ", e);
                            youTubePlayer.release();
                            Toast.makeText(getContext(), "Ekkert myndband til", Toast.LENGTH_LONG).show();
                            return;
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

                //View customView = inflater.inflate(R.layout.showtimes, null);


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
                        int margin = 20;
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
                            addUrlToBtn(schedule, schedules.getJSONObject(j).getString("purchase_url"));
                            schedule.setLayoutParams(new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        TextView ratingView = (TextView) mView.findViewById(R.id.movieRating);
        ImageView certView = (ImageView) mView.findViewById(R.id.movieCertificate);

        Picasso.with(getContext()).load(movie.getPoster()).into(imageView);
        titleView.setText(movie.getTitle());
        descrView.setText(movie.getDescr());
        ratingView.setText(movie.getImdb());
        Picasso.with(getContext()).load(movie.getCert()).into(certView);
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
