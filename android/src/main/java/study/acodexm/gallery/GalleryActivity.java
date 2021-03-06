package study.acodexm.gallery;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import acodexm.panorama.R;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GalleryActivity extends Activity {
    public static final String INTENT_EXTRAS_FOLDER = "panorama_app_folder";
    private static final String TAG = GalleryActivity.class.getName();
    private ViewFlipper viewFlipper;
    private ImageView currentView;
    private String imagesFolder;
    private NavigableMap<Double, String> imagesPath;
    private double current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.gallery);
        ButterKnife.bind(this);
        viewFlipper = findViewById(R.id.gallery_view_flipper);
        current = 0;
        loadImages();
        start();
        //attaching SwipeListener to view
        viewFlipper.setOnTouchListener(new SwipeListener(new Runnable() {
            @Override
            public void run() {
                previousImage();
            }
        }, new Runnable() {
            @Override
            public void run() {
                nextImage();
            }
        }));

    }

    /**
     * method used to create current, next and previous view
     *
     * @param activity
     * @return
     */
    public ImageView createImageView(Activity activity) {
        ImageView imageView = new ImageView(activity);
        imageView.setLayoutParams(new GridView.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return imageView;
    }


    public void setSlideToRightAnimation(ViewFlipper flipper,
                                         Activity context) {
        flipper.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_left));
    }

    public void setSlideToLeftAnimation(ViewFlipper flipper,
                                        Activity context) {
        flipper.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_left));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_right));
    }

    @OnClick(R.id.delete_picture)
    void onTrashClickListener() {
        File picToTrash = new File(imagesPath.get(current));
        if (picToTrash.delete()) {
            imagesPath.remove(current);
            nextImage();
        } else {
            Log.d(TAG, "onTrashClickListener: failed to delete file!");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentView != null) {
            return currentView.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }

    /**
     * method loads pictures from PanoramaApp folder
     */
    private void loadImages() {
        Bundle extras = getIntent().getExtras();
        imagesPath = new TreeMap<>();
        if (extras != null) {
            String extrasFolder = extras.getString(INTENT_EXTRAS_FOLDER);
            if (extrasFolder != null)
                imagesFolder = extrasFolder;
        }
        Log.d(TAG, "load images from imagesFolder:" + imagesFolder);
        if (imagesFolder != null) {
            File file = new File(imagesFolder);
            Log.d(TAG, "loadImages file exist: " + file.exists());
            Log.d(TAG, "loadImages file id folder: " + file.isDirectory());
            if (file.exists() && file.isDirectory()) {
                File[] listFiles = file.listFiles();
                for (File fileCurrent : listFiles) {
                    if (fileCurrent.isFile()) {
                        Log.d(TAG, "loadImages added file:" + fileCurrent.getPath());
                        Pattern num = Pattern.compile("\\d+");
                        int offset = fileCurrent.getPath().indexOf("panorama_");
                        //if found file is not named correctly than ignore that file
                        if (offset == -1) continue;
                        Matcher mN = num.matcher(fileCurrent.getPath().substring(offset));
                        double date;
                        if (mN.find()) {
                            date = Double.parseDouble(mN.group());
                            Log.d(TAG, "number found: " + date);
                            imagesPath.put(date, fileCurrent.getPath());
                            if (current < date)
                                current = date;
                        }
                    }
                }
            }
            Log.d(TAG, "loadImages images count :" + imagesPath.size());
        }

    }

    /**
     * create new view with the newest taken picture
     */
    private void start() {
        if (imagesPath.size() > 0) {
            currentView = createImageView(this);
            viewFlipper.addView(currentView);
            loadImageInView(currentView, imagesPath.get(current));
            viewFlipper.setDisplayedChild(0);
        } else {
            Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
    }

    /**
     * create new view with next picture
     */
    private void nextImage() {
        setSlideToLeftAnimation(viewFlipper, this);
        currentView = createImageView(this);
        viewFlipper.addView(currentView);
        String picture;
        Map.Entry<Double, String> next = imagesPath.higherEntry(current);
        if (next == null) {
            picture = imagesPath.firstEntry().getValue();
            current = imagesPath.firstEntry().getKey();
        } else {
            picture = imagesPath.higherEntry(current).getValue();
            current = imagesPath.higherEntry(current).getKey();
        }
        if (picture != null) {
            loadImageInView(currentView, picture);
            viewFlipper.showNext();
            viewFlipper.removeViewAt(0);
        } else {
            Log.d(TAG, "no more pictures!");
        }
    }

    /**
     * create new view with previous picture
     */
    private void previousImage() {
        setSlideToRightAnimation(viewFlipper, this);
        currentView = createImageView(this);
        viewFlipper.addView(currentView);
        String picture;
        Map.Entry<Double, String> prev = imagesPath.lowerEntry(current);
        if (prev == null) {
            picture = imagesPath.lastEntry().getValue();
            current = imagesPath.lastEntry().getKey();
        } else {
            picture = imagesPath.lowerEntry(current).getValue();
            current = imagesPath.lowerEntry(current).getKey();
        }
        if (picture != null) {
            loadImageInView(currentView, picture);
            viewFlipper.showNext();
            viewFlipper.removeViewAt(0);
        } else {
            Log.d(TAG, "no more pictures!");
        }

    }

    /**
     * method loads picture from storage to specified view
     *
     * @param imageView
     * @param path
     */
    public void loadImageInView(ImageView imageView, String path) {
        try {
            Bitmap decodeStream;
            FileInputStream fileInputStream = new FileInputStream(path);
            decodeStream = BitmapFactory.decodeStream(fileInputStream);
            imageView.setImageBitmap(decodeStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException " + e);
        }
    }
}