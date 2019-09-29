package com.demo.arcore.sceneform;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;


public class ArVideoFragment extends ArFragment {
//https://developers.google.com/ar/develop/java/augmented-images/guide
    private final static String TAG = "ArVideoFragment";

    private MediaPlayer mediaPlayer;
    private ExternalTexture externalTexture;
    private ModelRenderable videoRenderable;
    private AnchorNode videoAnchorNode;
    private AugmentedImage activeAugmentedImage;

    private final static String TEST_IMAGE_1 = "test_image_1.jpg";
    private final static String TEST_IMAGE_2 = "test_image_2.jpg";
    private final static String TEST_IMAGE_3 = "test_image_3.jpg";

    private final static String TEST_VIDEO_1 = "test_video_1.mp4";
    private final static String TEST_VIDEO_2 = "test_video_2.mp4";
    private final static String TEST_VIDEO_3 = "test_video_3.mp4";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view= super.onCreateView(inflater, container, savedInstanceState);

        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);
        getArSceneView().setLightEstimationEnabled(false);
        initializeSession();
        createArScene();
        return  view;
    }

    private final void createArScene() {
        externalTexture = new ExternalTexture();
//        MediaPlayer mediaPlayer_ = this.mediaPlayer;
        if (mediaPlayer == null) {
            // error
        }

        mediaPlayer.setSurface(externalTexture.getSurface());

        ModelRenderable.builder().setSource(this.requireContext(), R.raw.augmented_video_model).build().thenAccept(new Consumer() {
            // $FF: synthetic method
            // $FF: bridge method
            public void accept(Object var1) {
                this.accept((ModelRenderable)var1);
            }

            public final void accept(ModelRenderable renderable) {
                videoRenderable = renderable;
                renderable.setShadowCaster(false);
                renderable.setShadowReceiver(false);
                renderable.getMaterial().setExternalTexture("videoTexture", externalTexture);
            }
        }).exceptionally(throwable -> {
            Toast toast =
                    Toast.makeText(getContext(), "Unable to load andy renderable", Toast.LENGTH_LONG);

            toast.show();
            return null;
        } );

        videoAnchorNode = new AnchorNode();
        videoAnchorNode.setParent(getArSceneView().getScene());
    }

    @Override
    protected Config getSessionConfiguration(Session session) {

        Config config = super.getSessionConfiguration(session);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
        config.setFocusMode(Config.FocusMode.AUTO);
        if (!setupAugmentedImageDatabase(config, session)) {
            Toast.makeText(this.requireContext(), "Could not setup augmented image database", Toast.LENGTH_LONG).show();
        }

        return config;
    }
    
    private Bitmap loadAugmentedImageBitmap(String imageName){
        Context context = ArVideoFragment.this.requireContext();
        Bitmap bitmap = null;
        try {
            InputStream it = context.getAssets().open(imageName);
            bitmap = BitmapFactory.decodeStream(it);
            
        } catch (IOException e) {
            Log.e(TAG,"loadAugmentedImageBitmap fail: "+ e.toString());
            e.printStackTrace();
        }
        return bitmap;
    }
    
    private Boolean setupAugmentedImageDatabase(Config config, Session session){
        try {
            AugmentedImageDatabase database = new AugmentedImageDatabase(session);
            database.addImage(TEST_VIDEO_1, loadAugmentedImageBitmap(TEST_IMAGE_1));
            database.addImage(TEST_VIDEO_2, loadAugmentedImageBitmap(TEST_IMAGE_2));
            database.addImage(TEST_VIDEO_3, loadAugmentedImageBitmap(TEST_IMAGE_3));
            config.setAugmentedImageDatabase(database);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not add bitmap to augmented image database", e);
        }
        return false;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = getArSceneView().getArFrame();
        if (frame != null) {
            Camera camera = frame.getCamera();
            if (camera.getTrackingState() == TrackingState.TRACKING) {
                Collection<AugmentedImage> updatedAugmentedImages =
                        frame.getUpdatedTrackables(AugmentedImage.class);
                for(AugmentedImage augmentedImage : updatedAugmentedImages){

                    if (augmentedImage != activeAugmentedImage  &&
                            (activeAugmentedImage ==null || !activeAugmentedImage.getName().equals(augmentedImage.getName()))  &&
                            augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                        Log.e("ArVideoFragment", "Tracking_augmented image [" + augmentedImage.getName() + ']' );
                        if(activeAugmentedImage !=null){
                            Log.e("ArVideoFragment", "Tracking_augmented activeAugmentedImage [" + activeAugmentedImage.getName() + ']' );
                        }
                        try {
                            dismissArVideo();
                            playbackArVideo(augmentedImage);
                            break;
                        } catch (Exception e) {
                            Log.e(TAG, "Could not play video [${augmentedImage.name}]", e);
                        }
                    }
                }

                return;
            }
        }
    }

    private void dismissArVideo() {
        if(videoAnchorNode !=null){
            if(videoAnchorNode.getAnchor() !=null){
                videoAnchorNode.getAnchor().detach();
            }
            videoAnchorNode.setRenderable(null);
        }
        activeAugmentedImage = null;
        mediaPlayer.reset();
        Log.e("ArVideoFragment", "dismissArVideo");
    }

    private void playbackArVideo(AugmentedImage augmentedImage) {
        Log.e("ArVideoFragment", "playbackVideo = " + augmentedImage.getName());

        AssetFileDescriptor descriptor = null;
        try {
            descriptor = requireContext().getAssets().openFd(augmentedImage.getName());
            try {
//                MediaPlayer.create(getContext(), Uri.parse("http://easyhtml5video.com/assets/video/new/Penguins_of_Madagascar.mp4"));
                if (mediaPlayer == null) {
                    // mediaPlayer null
                }
//                mediaPlayer.setDataSource(descriptor);
                mediaPlayer.setDataSource("http://easyhtml5video.com/assets/video/new/Penguins_of_Madagascar.mp4");
            } catch (Throwable throwable) {
                Log.e("ArVideoFragment", "playbackArVideo Throwable: " + throwable.toString());
                throw throwable;
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.e(TAG, "onCompletion");

                }
            });
            mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (extra == MediaPlayer.MEDIA_ERROR_SERVER_DIED
                            || extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
                        Log.e(TAG, "erroronplaying MEDIA_ERROR_SERVER_DIED");
                    } else if (extra == MediaPlayer.MEDIA_ERROR_IO) {
                        Log.e(TAG, "erroronplaying MEDIA_ERROR_IO");
                        return false;
                    }
                    return false;
                }
            });
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    Log.e(TAG, "onBufferingUpdate percent: " + percent);

                }
            });


            videoAnchorNode.setAnchor(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
            videoAnchorNode.setLocalScale(new Vector3(augmentedImage.getExtentX(), 1.0F, augmentedImage.getExtentZ()));
            activeAugmentedImage = augmentedImage;
            Log.e("ArVideoFragment", "playbackArVideo activeAugmentedImage: " + activeAugmentedImage.getName());
            externalTexture.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                public final void onFrameAvailable(SurfaceTexture it) {
                    it.setOnFrameAvailableListener(null);
                    videoAnchorNode.setRenderable(videoRenderable);
                }
            });
        } catch (IOException e) {
            Log.e("ArVideoFragment", "playbackArVideo IOException: " + e.toString());
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        dismissArVideo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }
}
