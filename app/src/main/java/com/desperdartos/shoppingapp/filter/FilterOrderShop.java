package com.desperdartos.shoppingapp.filter;

import android.widget.Filter;

import com.desperdartos.shoppingapp.adapters.AdapterOrderShop;
import com.desperdartos.shoppingapp.adapters.AdapterProductSeller;
import com.desperdartos.shoppingapp.models.ModelOrderShop;
import com.desperdartos.shoppingapp.models.ModelProduct;

import java.util.ArrayList;

import lombok.AllArgsConstructor;

//@AllArgsConstructor
public class FilterOrderShop extends Filter{
    private AdapterOrderShop adapter;
    private ArrayList<ModelOrderShop> filterList;

    public FilterOrderShop(AdapterOrderShop adapter, ArrayList<ModelOrderShop> filterList){
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults result = new FilterResults();
        //validate data for search query
        if (constraint != null && constraint.length() > 0) {
            //now change to upper case to lower case
            constraint = constraint.toString().toUpperCase();
            // now store in filter list
            ArrayList<ModelOrderShop> filteredModels = new ArrayList<>();
            for (int i = 0; i < filterList.size(); i++) {
                //check search by title and category
                if (filterList.get(i).getOrderStatus().toUpperCase().contains(constraint)) {
                    //add filter data to list
                    filteredModels.add(filterList.get(i));
                }
            }
            result.count = filteredModels.size();
            result.values = filteredModels;
        }else {
            //search filter empty ,return original product list
            result.count = filterList.size();
            result.values = filterList;
        }
        return result;
    }

    @Override
    protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        adapter.orderShopArrayList = (ArrayList<ModelOrderShop>) results.values;
        //refresh adapter
        adapter.notifyDataSetChanged();
    }
}

