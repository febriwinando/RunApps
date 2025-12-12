package tech.id.runappsandroid;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction>
        implements Filterable {

    private List<AutocompletePrediction> data;
    private final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
    private final PlacesClient placesClient;

    public PlacesAutoCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        data = new ArrayList<>();
    }

    @Override
    public int getCount() { return data.size(); }

    @Override
    public AutocompletePrediction getItem(int position) { return data.get(position); }

    // === FIX UTAMA: tampilkan nama lokasi ===
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView tv = view.findViewById(android.R.id.text1);

        AutocompletePrediction item = getItem(position);
        if (item != null) {
            // bisa pakai primaryText atau fullText
            tv.setText(item.getPrimaryText(null));   // Nama tempat
        }

        return view;
    }

    // ==== FILTERING AUTOCOMPLETE ====
    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint != null && constraint.length() > 0) {

                    FindAutocompletePredictionsRequest request =
                            FindAutocompletePredictionsRequest.builder()
                                    .setSessionToken(token)
                                    .setQuery(constraint.toString())
                                    .build();

                    Task<FindAutocompletePredictionsResponse> task =
                            placesClient.findAutocompletePredictions(request);

                    try {
                        FindAutocompletePredictionsResponse response =
                                Tasks.await(task, 2, TimeUnit.SECONDS);

                        data = response.getAutocompletePredictions();

                        results.values = data;
                        results.count = data.size();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((AutocompletePrediction) resultValue).getPrimaryText(null);
            }
        };
    }
}
