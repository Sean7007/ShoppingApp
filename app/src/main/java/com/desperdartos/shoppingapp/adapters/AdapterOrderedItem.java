package com.desperdartos.shoppingapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.models.ModelCartItem;
import com.desperdartos.shoppingapp.models.ModelOrderedItem;

import java.util.ArrayList;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AdapterOrderedItem extends RecyclerView.Adapter<AdapterOrderedItem.HolderOrderedItem> {
    Context context;
    ArrayList<ModelOrderedItem> orderedItemArrayList;

   /* public AdapterOrderedItem(Context context, ArrayList<ModelOrderedItem> orderedItemArrayList) {
        this.context = context;
        this.orderedItemArrayList = orderedItemArrayList;
    }*/

    @NonNull
    @Override
    public HolderOrderedItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_ordereditem, parent, false);
        return new HolderOrderedItem(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderOrderedItem holder, int position) {
        ModelOrderedItem modelCartItem = orderedItemArrayList.get(position);
        String getPid = modelCartItem.getPId();
        String name = modelCartItem.getName();
        String cost = modelCartItem.getCost();
        String price = modelCartItem.getPrice();
        String quantity = modelCartItem.getQuantity();
        //now set data to view
        holder.itemTitleTv.setText(name);
        holder.itemPriceEachTv.setText(""+price);
        holder.itemPriceTv.setText(""+cost);
        holder.itemQuanEachTv.setText("["+quantity+"]");
    }

    @Override
    public int getItemCount() {
        return orderedItemArrayList.size();
    }

    class HolderOrderedItem extends RecyclerView.ViewHolder{
        TextView itemTitleTv, itemPriceTv, itemPriceEachTv, itemQuanEachTv;

        public HolderOrderedItem(@NonNull View itemView) {
            super(itemView);
            itemTitleTv = itemView.findViewById(R.id.itemTitleTv);
            itemPriceTv = itemView.findViewById(R.id.itemPriceTv);
            itemPriceEachTv = itemView.findViewById(R.id.itemPriceEachTv);
            itemQuanEachTv = itemView.findViewById(R.id.itemQuanEachTv);
        }
    }
}
