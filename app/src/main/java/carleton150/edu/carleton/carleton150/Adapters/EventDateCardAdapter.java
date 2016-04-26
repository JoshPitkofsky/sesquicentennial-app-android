package carleton150.edu.carleton.carleton150.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fortuna.ical4j.model.Calendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.Interfaces.RecyclerViewDatesClickListener;
import carleton150.edu.carleton.carleton150.R;


/**
 * Adapter for the RecyclerView that shows images in the bottom of the HistoryFragment
 */
public class EventDateCardAdapter extends RecyclerView.Adapter<EventDateCardAdapter.EventDateCardViewHolder> {

    public static RecyclerViewDatesClickListener clickListener;
    private int screenWidth;
    private ArrayList<String> dateInfo;


    public EventDateCardAdapter(ArrayList<String> dateInfo, RecyclerViewDatesClickListener clickListener,
                                int screenWidth) {

        this.clickListener = clickListener;
        this.screenWidth = screenWidth;
        this.dateInfo = dateInfo;
    }



    @Override
    public EventDateCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.event_date_card, parent, false);
        return new EventDateCardViewHolder(itemView);
    }


    // Setting view for each card
    @Override
    public void onBindViewHolder(EventDateCardViewHolder holder, int position) {
        holder.setDate(dateInfo.get(position));
        holder.setWidth((int) (screenWidth / 2.5));
    }

    @Override
    public int getItemCount() {
        if(dateInfo != null) {
            return dateInfo.size();
        }else{
            return 0;
        }
    }

    public static class EventDateCardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public EventDateCardViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

        }

        /**
         * @param width
         */
        public void setWidth(int width) {
            itemView.setLayoutParams(new RecyclerView.LayoutParams(width, RecyclerView.LayoutParams.MATCH_PARENT));
        }

        @Override
        public void onClick(View v) {
            TextView dateTitle = (TextView) itemView.findViewById(R.id.event_date_title);
            String dateInfo = dateTitle.getTag().toString();

            clickListener.recyclerViewListClicked(dateInfo);

        }

        // Set date in event calendar date tabs
        public void setDate(String dateInfo) {
            TextView dateTitle = (TextView) itemView.findViewById(R.id.event_date_title);

            DateFormat dfCorrect = new SimpleDateFormat("EEE'\r\n'MMM dd',' yyyy", Locale.US);

            String[] dateArray = dateInfo.split("-");

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]));
            Date curDate = calendar.getTime();


            dateTitle.setText(dfCorrect.format(curDate));
            dateTitle.setTag(dateInfo);
        }

    }
}