package com.example.inmobiliariaventa.ui.home;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inmobiliariaventa.R;
import java.util.List;

public class LocationSuggestionAdapter extends RecyclerView.Adapter<LocationSuggestionAdapter.ViewHolder> {

    private List<String> suggestions;

    public LocationSuggestionAdapter(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.suggestionTextView.setText(Html.fromHtml(suggestion, Html.FROM_HTML_MODE_COMPACT));
    }

    @Override
    public int getItemCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionTextView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionTextView = itemView.findViewById(R.id.suggestionTextView);
        }
    }
}
