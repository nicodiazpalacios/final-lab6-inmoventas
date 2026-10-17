package com.example.inmobiliariaventa.ui.home;

import android.content.Context;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.R;
import java.util.ArrayList;
import java.util.List;

public class LocationSuggestionAdapter extends RecyclerView.Adapter<LocationSuggestionAdapter.ViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(SuggestionItem item);
    }

    public static class SuggestionItem {
        public final String displayHtml;
        public final String searchQuery;
        public final String filterTipo;
        public final boolean isHeader;

        public SuggestionItem(String displayHtml, String searchQuery, String filterTipo, boolean isHeader) {
            this.displayHtml = displayHtml;
            this.searchQuery = searchQuery != null ? searchQuery : "";
            this.filterTipo = filterTipo;
            this.isHeader = isHeader;
        }

        public static SuggestionItem header(String title) {
            return new SuggestionItem("<small><b>" + title + "</b></small>", "", null, true);
        }

        public static SuggestionItem item(String displayHtml, String searchQuery) {
            return new SuggestionItem(displayHtml, searchQuery, null, false);
        }

        public static SuggestionItem item(String displayHtml, String searchQuery, String filterTipo) {
            return new SuggestionItem(displayHtml, searchQuery, filterTipo, false);
        }
    }

    private List<SuggestionItem> items;
    private OnSuggestionClickListener listener;
    private java.util.Set<String> selectedLocations = new java.util.HashSet<>();

    public LocationSuggestionAdapter(List<SuggestionItem> items, OnSuggestionClickListener listener) {
        this.items = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<SuggestionItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedLocations(java.util.Set<String> selected) {
        this.selectedLocations = selected != null ? new java.util.HashSet<>(selected) : new java.util.HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SuggestionItem item = items.get(position);
        holder.suggestionTextView.setText(Html.fromHtml(item.displayHtml, Html.FROM_HTML_MODE_COMPACT));
        Context context = holder.itemView.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        if (item.isHeader) {
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
            holder.itemView.setBackground(null);
            if (holder.imgCheckLocation != null) {
                holder.imgCheckLocation.setVisibility(View.GONE);
            }
            holder.suggestionTextView.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
            holder.suggestionTextView.setPadding((int)(16 * density), (int)(16 * density), (int)(16 * density), (int)(6 * density));
            holder.suggestionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        } else {
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            holder.itemView.setBackgroundResource(outValue.resourceId);
            holder.suggestionTextView.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
            holder.suggestionTextView.setPadding((int)(16 * density), (int)(14 * density), (int)(16 * density), (int)(14 * density));
            holder.suggestionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

            boolean isSelected = false;
            if (selectedLocations != null && item.searchQuery != null) {
                for (String sel : selectedLocations) {
                    if (sel.equalsIgnoreCase(item.searchQuery.trim())) {
                        isSelected = true;
                        break;
                    }
                }
            }
            if (holder.imgCheckLocation != null) {
                holder.imgCheckLocation.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionTextView;
        android.widget.ImageView imgCheckLocation;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionTextView = itemView.findViewById(R.id.suggestionTextView);
            imgCheckLocation = itemView.findViewById(R.id.imgCheckLocation);
        }
    }
}
