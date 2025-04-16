package com.example.watacrab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.models.WaterLog;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WaterLogAdapter extends RecyclerView.Adapter<WaterLogAdapter.WaterLogViewHolder> {
    private List<WaterLog> waterLogs;
    private SimpleDateFormat timeFormat;

    public WaterLogAdapter(List<WaterLog> waterLogs) {
        this.waterLogs = waterLogs;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public WaterLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_log, parent, false);
        return new WaterLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaterLogViewHolder holder, int position) {
        WaterLog waterLog = waterLogs.get(position);
        holder.timeText.setText(timeFormat.format(waterLog.getTimestamp()));
        holder.amountText.setText(String.format("%d ml", waterLog.getAmount()));
        holder.noteText.setText(waterLog.getNote());
    }

    @Override
    public int getItemCount() {
        return waterLogs.size();
    }

    static class WaterLogViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        TextView amountText;
        TextView noteText;

        WaterLogViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
            amountText = itemView.findViewById(R.id.amountText);
            noteText = itemView.findViewById(R.id.noteText);
        }
    }
} 