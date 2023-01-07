package com.example.memorygame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ListItemClickListener {

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;
    ProgressBar progressBar;
    TextView textView;
    TextView helpText;
    Thread imageProcess = null;
    Button startButton;
    Button fetchButton;
    Handler mHandler = new Handler();
    AutoCompleteTextView input;
//    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);


        recyclerView = findViewById(R.id.recyclerView);
        recyclerAdapter = new RecyclerAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(recyclerAdapter);

        progressBar = findViewById(R.id.progressBar);

        helpText = findViewById(R.id.helpText);

//        findViewById(R.id.scrollText).setSelected(true);

        // Initializing autocomplete for editText
        input = findViewById(R.id.textInputEditText);
        String[] websites = getResources().getStringArray(R.array.suggested_urls);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, websites);
        input.setAdapter(adapter);

        textView = findViewById(R.id.textView);

        fetchButton = findViewById(R.id.fetchButton);
        fetchButton.setOnClickListener(this);

        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();


        recyclerAdapter.clearImages();
        progressBar.setVisibility(View.INVISIBLE);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        textView.setVisibility(View.INVISIBLE);
        fetchButton.setEnabled(true);
        helpText.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        startButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Deletes files on full exit from application
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        for (int i = 1; i <= 6; i++) {
            File file = new File(dir, "selectedImage" + i + ".jpg");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        int buttonId = v.getId();

        if (buttonId == R.id.fetchButton) {
            // Scrape website and populate GridView
            hideKeyboard(this);
            if (input.getText() != null) {
                if (imageProcess != null) {
                    imageProcess.interrupt();
                    try {
                        imageProcess.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                imageProcess = new Thread(new LoadImagesTask(input.getText().toString()));
                imageProcess.start();

            }

        } else if (buttonId == R.id.startButton) {
            // Start button implementation
            Thread imageDownload = new Thread(new SaveImagesTask(recyclerAdapter.getSelectedImages()));
            imageDownload.start();

        }
    }

    @Override
    public void onListItemClick(int position) {

        if (recyclerAdapter.getSelectedImages().size() == 6) {
            startButton.setEnabled(true);
        } else if (startButton.isEnabled()) {
            startButton.setEnabled(false);
        }
    }

    private class LoadImagesTask implements Runnable {
        private String url;
        boolean useGlide = true;
        boolean useJsoup = true;

        private ArrayList<String> images;

        LoadImagesTask(String url) {
            // Trim trailing slashes in constructor
            this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        }

        @Override
        public void run() {
            try {
                if (!URLUtil.isValidUrl(url)) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Invalid URL, please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                startUI();

                if (useJsoup) {
                    // Included because it is interesting, in our tests it was found
                    // that difference in performance was trivial (~15 ms vs ~60 ms)
                    // but jsoup parses more accurately
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.select("img[src~=(?i).(png|jpe?g)]");
                    images = (ArrayList<String>) elements.stream()
                            .map(e -> e.attr("src").startsWith("http") ? e.attr("src") : url + e.attr("src"))
                            .collect(Collectors.toList());
                }

                progressBar.setMax(20);
                for (int i = 0; i < 20; i++) {
                    if (Thread.interrupted()) {
                        concludeUI(false);
                        break;
                    }

                    String sourceAttribute = images.get(i);
                    updateUI(i + 1, sourceAttribute);
                }

                concludeUI(true);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                concludeUI(false);
            }

        }

        private void startUI() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    recyclerView.getRecycledViewPool().clear();
                    recyclerAdapter.clearImages();
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                    textView.setText("Starting to process images..");
                    textView.setVisibility(View.VISIBLE);
                    startButton.setVisibility(View.INVISIBLE);
                    helpText.setVisibility(View.INVISIBLE);
                }
            });
        }

        private void updateUI(int progress, String url) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap bmp;
                        if (useGlide) {
                            bmp = Glide.with(getApplicationContext()).asBitmap().load(url).submit().get();
                        } else {
                            bmp = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
                        }
                        Bitmap finalBmp = bmp;
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                progressBar.setProgress(progress);
                                textView.setText(String.format(Locale.ENGLISH, "Downloading image %d of %d...", progress, 20));
                                recyclerAdapter.addImage(url, finalBmp);
                            }
                        });
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }).run();


        }

        private void concludeUI(boolean success) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Images processed!", Toast.LENGTH_SHORT).show();
                    } else {
                        recyclerAdapter.clearImages();
                        Toast.makeText(getApplicationContext(), "Image loading failed!", Toast.LENGTH_SHORT).show();
                    }

                    imageProcess = null;
                    progressBar.setVisibility(View.INVISIBLE);
                    textView.setVisibility(View.INVISIBLE);
                    if (success) {
                        startButton.setVisibility(View.VISIBLE);
                        helpText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private class SaveImagesTask implements Runnable {
        private List<BitmapDrawable> bitmaps;

        SaveImagesTask(List<BitmapDrawable> bitmaps) {
            this.bitmaps = bitmaps;
        }

        @Override
        public void run() {
            startUI();

            for (int i = 0; i < bitmaps.size(); i++) {
                if (Thread.interrupted()) {
                    concludeUI(false);
                    break;
                }

                updateUI(i + 1);

                if (!save(bitmaps.get(i).getBitmap(), "selectedImage" + (i + 1) + ".jpg")) {
                    Thread.currentThread().interrupt();
                    concludeUI(false);
                    break;
                }
            }

            concludeUI(true);

        }

        public boolean save(Bitmap image, String filename) {
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = new File(dir, filename);

            if (file.exists()) {
                file.delete();
            }

            try {
                FileOutputStream out = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private void startUI() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fetchButton.setEnabled(false);
                    startButton.setVisibility(View.INVISIBLE);
                    progressBar.setProgress(0);
                    progressBar.setMax(bitmaps.size());
                    progressBar.setVisibility(View.VISIBLE);
                    textView.setText("Starting to download images..");
                    textView.setVisibility(View.VISIBLE);
                }
            });
        }

        private void updateUI(int progress) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(progress);
                    textView.setText(String.format(Locale.ENGLISH, "Saving image %d of %d...", progress, bitmaps.size()));
                }
            });

        }

        private void concludeUI(boolean success) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                    textView.setVisibility(View.INVISIBLE);

                    if (success) {
                        Intent intent = new Intent(getApplicationContext(), GameActivity.class);
                        startActivity(intent);
                    } else {
                        startButton.setVisibility(View.VISIBLE);
                        fetchButton.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Downloading of images failed, please try again", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}