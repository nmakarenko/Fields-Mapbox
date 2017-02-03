package com.cropiotest.fields;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cropiotest.fields.model.Field;

import java.util.List;
import java.util.Locale;

public class FieldsRVAdapter extends RecyclerView.Adapter<FieldsRVAdapter.ViewHolder> {
    Context context;
    List<Field> data;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public FieldsRVAdapter(Context context, List<Field> fields, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        data = fields;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fields_list_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {

        viewHolder.tvName.setText(data.get(i).name);
        viewHolder.tvCrop.setText(data.get(i).crop);
        viewHolder.tvTillArea.setText(String.format(Locale.US, "%d ha", (int) data.get(i).tillArea));

        final int position = i;
        viewHolder.llItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout llItem;

        TextView tvName;
        TextView tvCrop;
        TextView tvTillArea;

        ViewHolder(View itemView) {
            super(itemView);
            llItem = (LinearLayout) itemView.findViewById(R.id.llItem);

            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvCrop = (TextView) itemView.findViewById(R.id.tvCrop);
            tvTillArea = (TextView) itemView.findViewById(R.id.tvTillArea);
        }
    }
}
