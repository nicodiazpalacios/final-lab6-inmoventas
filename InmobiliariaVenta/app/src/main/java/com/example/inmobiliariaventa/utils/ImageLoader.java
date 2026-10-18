package com.example.inmobiliariaventa.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImageLoader {

    /**
     * Carga recortando al centro para tarjetas y cabeceras de listados.
     */
    public static void loadImage(ImageView imageView, String url) {
        if (imageView == null) return;
        Context context = imageView.getContext();
        if (context == null) return;

        if (url == null || url.trim().isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        String optimizedUrl = optimizeUrl(url.trim());

        try {
            Glide.with(context)
                    .load(optimizedUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .override(720, 480)
                    .centerCrop()
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
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        String optimizedUrl = optimizeUrlFull(url.trim());

        try {
            Glide.with(context)
                    .load(optimizedUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .fitCenter()
                    .into(imageView);
        } catch (Exception ignored) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private static String optimizeUrl(String url) {
        if (url != null && url.contains("images.unsplash.com") && !url.contains("w=")) {
            return url + (url.contains("?") ? "&" : "?") + "w=700&q=75&auto=format";
        }
        return url;
    }

    private static String optimizeUrlFull(String url) {
        if (url != null && url.contains("images.unsplash.com") && !url.contains("w=")) {
            return url + (url.contains("?") ? "&" : "?") + "w=1600&q=85&fit=inside&auto=format";
        }
        return url;
    }
}
