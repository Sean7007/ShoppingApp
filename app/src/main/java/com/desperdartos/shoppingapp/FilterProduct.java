package com.desperdartos.shoppingapp;

import android.widget.Filter;

import com.desperdartos.shoppingapp.models.ModelProduct;
import com.desperdartos.shoppingapp.adapters.AdapterProductSeller;

import java.util.ArrayList;

public class FilterProduct extends Filter {

    private AdapterProductSeller adapter;
    private ArrayList<ModelProduct> filterList;

    public FilterProduct(AdapterProductSeller adapter, ArrayList<ModelProduct> filterList){
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults results = new FilterResults();
        //Validate data for search query
        if (charSequence != null && charSequence.length() > 0){
            //Search filed not empty, searching something do search



            //Change to upper case, to make case insensitive
            charSequence = charSequence.toString().toUpperCase();
            //Store our filtered list
            ArrayList<ModelProduct> filteredModels = new ArrayList<>();
            for(int i=0; i < filterList.size(); i++){
                //check
                if (filterList.get(i).getProductCategory().toUpperCase().contains(charSequence) ||
                        filterList.get(i).getProductCategory().toUpperCase().contains(charSequence)){
                    //Add filtered data to list
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        }else{
            //Search filed empty, not searching return original list

            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        adapter.productList = (ArrayList<ModelProduct>) filterResults.values;
        //refresh adapter
        adapter.notifyDataSetChanged();
    }
}
