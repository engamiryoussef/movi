package com.example.root.miro;


import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    public ImageAdapter image;
    public List<Movie> movieList;
    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FetchMoviesTask fetch = new FetchMoviesTask();
        fetch.execute("test");
        image = new ImageAdapter(getContext(), new ArrayList<Movie>());
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(image);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                String movie=movieList.get(position).toString();
                Intent intent = new Intent(getActivity(), DetailsActivity.class).putExtra(Intent.EXTRA_TEXT, movie);
                startActivity(intent);
            }
        });
        return rootView;
    }

    public class FetchMoviesTask extends AsyncTask<String, Void, List<Movie>> {
        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        private List<Movie> getMovieDataFromJson(String movieJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "results";
            final String OWM_POSTER = "poster_path";
            final String OWM_RELEASEDATE = "release_date";
            final String OWM_DESCRIPTION = "overview";
            final String OWM_TITLE = "original_title";
            final String OWM_LANGUAGE = "original_language";
            final String OWM_MOST_POPULAR = "popularity";
            final String OWM_HIGHEST_RATE = "vote_count";
            final String OWM_ID="id";
            JSONObject forecastJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.
            Movie movie_var;
            List<Movie> result = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                // For now, using the format "Day, description, hi/low"
                movie_var = new Movie();
                // Get the JSON object representing the day
                JSONObject movie = movieArray.getJSONObject(i);
                movie_var.setPoster(movie.getString(OWM_POSTER));
                movie_var.setTitle(movie.getString(OWM_TITLE));
                movie_var.setDescription(movie.getString(OWM_DESCRIPTION));
                movie_var.setRelease_date(movie.getString(OWM_RELEASEDATE));
                movie_var.setLanguage(movie.getString(OWM_LANGUAGE));
                movie_var.setPopular(movie.getString(OWM_MOST_POPULAR));
                movie_var.setRate(movie.getString(OWM_HIGHEST_RATE));
                movie_var.setId(movie.getString(OWM_ID));
                result.add(movie_var);
            }

            for (Movie s : result) {
                Log.v(LOG_TAG, "Movie entry: " + s.toString());
            }
            return result;

        }

        @Override
        protected List<Movie> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            try {
                final String BASE_URL =
                        "http://api.themoviedb.org/3/movie/popular?";
                final String APPID_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIES_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                movieList=getMovieDataFromJson(forecastJsonStr);
                Log.e(LOG_TAG, forecastJsonStr);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return movieList;
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            if (movies != null)
                if (movies.size() > 0) {
                    image.updateAdapter(movies);
                    image.notifyDataSetChanged();
                    Log.e(LOG_TAG, "Loooooooad Imageeeees"+movies.size());
                }
        }
    }

}