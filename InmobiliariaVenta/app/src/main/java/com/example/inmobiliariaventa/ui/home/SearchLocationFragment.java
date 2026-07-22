package com.example.inmobiliariaventa.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.example.inmobiliariaventa.R;
import java.util.ArrayList;
import java.util.List;

public class SearchLocationFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_location, container, false);
        
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        RecyclerView suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        List<String> suggestions = new ArrayList<>();
        // TODO: Cargar sugerencias dinámicamente

        LocationSuggestionAdapter adapter = new LocationSuggestionAdapter(suggestions);
        suggestionsRecyclerView.setAdapter(adapter);

        return view;
    }
}
