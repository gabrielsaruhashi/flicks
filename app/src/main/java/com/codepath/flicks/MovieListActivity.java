package com.codepath.flicks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.codepath.flicks.models.Config;
import com.codepath.flicks.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

public class MovieListActivity extends AppCompatActivity {
    // constants

    // the base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";

    // the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";


    // tag for logging from this activity
    public final static String TAG = "MovieListActivity";

    // instance fields
    AsyncHttpClient client;



    // the list of currently playing movies
    ArrayList<Movie> movies;

    // the recycler view
    @BindView(R.id.rvMovies) RecyclerView rvMovies;

    // the adapter wired to the recycler view
    MovieAdapter adapter;

    // image config
    Config config;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        ButterKnife.bind(this);

        // initialize the client
        client = new AsyncHttpClient();

        // initialize the list of movies
        movies = new ArrayList<>();

        // initialize the adapter -- movies array cannot be reinitialized after this point
        adapter = new MovieAdapter(movies);

        // resolve the recycler view and connect a layout manager and the adapter

        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        // get the configuration on app creation
        getConfiguration();

        // get the now playing movie list
        // I cant put getNowPlaying() here because as it is async it can load before config
    }

    // get the list of currently playing movies from the API
    private void getNowPlaying() {
        //create the URL
        String url = API_BASE_URL + "/movie/now_playing";

        // set the request parameters
        RequestParams params = new RequestParams();

        params.put(API_KEY_PARAM, getString(R.string.api_key));

        // execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // load the results into a movie list
                try {
                    JSONArray results = response.getJSONArray("results");

                    // iterate through result set and create Movie objects

                    for (int i = 0; i < results.length(); i ++) {
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);

                        // notify adapter that a row was added
                        adapter.notifyItemInserted(movies.size() - 1);
                    }
                    Log.i(TAG, String.format("Loaded %s movies", results.length()));
                } catch (JSONException e) {
                    logError("Failed to parse now playing", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from now playing endpoint", throwable, true);
            }
        });
    }

    //get the configuration from the API
    private void getConfiguration() {
        //create the URL
        String url = API_BASE_URL + "/configuration";

        // set the request parameters
        RequestParams params = new RequestParams();

        params.put(API_KEY_PARAM, getString(R.string.api_key));

        // execute a GET request expecting a JSON response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    config = new Config(response);

                    Log.i(TAG,
                            String.format("Loaded configuration with imageBaseUlr %s and posterSize %s",
                                    config.getImageBaseURL(),
                                    config.getPosterSize()));


                    // pass config to adapter
                    adapter.setConfig(config);

                    // get movies playing
                    getNowPlaying();
                } catch (JSONException e) {
                    logError("Failed parsing configuration", e, true);
                }



            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed getting configuration", throwable, true);
            }
        });


    }

    // handle erros, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        // always log the error
        Log.e(TAG, message, error);
        // alert the user to avoid silent error
        if (alertUser) {
            // show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

        }
    }

}
