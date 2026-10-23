package com.example.inmobiliariaventa.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class ImageLoader {

    private static final RequestOptions CARD_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .format(DecodeFormat.PREFER_RGB_565)
            .override(600, 360)
            .centerCrop();

    private static final RequestOptions FULL_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .format(DecodeFormat.PREFER_RGB_565)
            .fitCenter();

    /**
     * Carga recortando al centro para tarjetas y cabeceras de listados.
     */
    public static void loadImage(ImageView imageView, String url) {
        if (imageView == null) return;
        Context context = imageView.getContext();
        if (context == null) return;

        if (url == null || url.trim().isEmpty()) {
            clear(imageView);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        String optimizedUrl = optimizeUrl(url.trim());

        try {
            Glide.with(context)
                    .load(optimizedUrl)
                    .apply(CARD_OPTIONS)
                    .into(imageView);
        } catch (Exception ignored) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    /**
     * Carga la imagen COMPLETA sin recortar (fitCenter) para visores a pantalla completa con zoom.
     */
    public static void loadImageFit(ImageView imageView, String url) {
        if (imageView == null) return;
        Context context = imageView.getContext();
        if (context == null) return;

        if (url == null || url.trim().isEmpty()) {
            clear(imageView);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        String optimizedUrl = optimizeUrlFull(url.trim());

        try {
            Glide.with(context)
                    .load(optimizedUrl)
                    .apply(FULL_OPTIONS)
                    .override(1280, 800)
                    .into(imageView);
        } catch (Exception ignored) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    public static void preload(Context context, String url) {
        if (context == null || url == null || url.trim().isEmpty()) return;
        try {
            String optimizedUrl = optimizeUrl(url.trim());
            Glide.with(context.getApplicationContext())
                    .load(optimizedUrl)
                    .apply(CARD_OPTIONS)
                    .preload(600, 360);
        } catch (Exception ignored) {}
    }

    public static void clear(ImageView imageView) {
        if (imageView == null) return;
        try {
            Context context = imageView.getContext();
            if (context != null) {
                Glide.with(context).clear(imageView);
            }
        } catch (Exception ignored) {}
    }

    private static String optimizeUrl(String url) {
        if (url != null && url.contains("images.unsplash.com") && !url.contains("w=")) {
            return url + (url.contains("?") ? "&" : "?") + "w=600&q=70&auto=format";
        }
        return url;
    }

    private static String optimizeUrlFull(String url) {
        if (url != null && url.contains("images.unsplash.com") && !url.contains("w=")) {
            return url + (url.contains("?") ? "&" : "?") + "w=1400&q=80&fit=inside&auto=format";
        }
        return url;
    }
}
