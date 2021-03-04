package com.desperdartos.shoppingapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.activities.ShopDetailsActivity;
import com.desperdartos.shoppingapp.models.ModelShop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AdapterShop extends RecyclerView.Adapter<AdapterShop.HolderShop> {
    //var declaration
    private Context context;
    private ArrayList<ModelShop> shopsList;

    public AdapterShop(Context context, ArrayList<ModelShop> shopsList) {
        this.context = context;
        this.shopsList = shopsList;
    }

    @NonNull
    @Override
    public HolderShop onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate layout row_shop.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_shop, parent, false);
        return new HolderShop(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderShop holder, int position) {
        //Get data
        ModelShop modelShop = shopsList.get(position);
        String accountType = modelShop.getAccountType();
        String address = modelShop.getAddress();
        String city = modelShop.getCity();
        String country = modelShop.getCountry();
        String deliveryFee = modelShop.getDeliveryFee();
        String email = modelShop.getEmail();
        String latitude = modelShop.getLatitude();
        String longitude = modelShop.getLongitude();
        String online = modelShop.getOnline();
        String name = modelShop.getName();
        String phone = modelShop.getPhone();
        final String uid = modelShop.getUid();
        String timeStamp = modelShop.getTimeStamp();
        String shopOpen = modelShop.getShopOpen();
        String profileImage = modelShop.getProfileImage();
        String shopName = modelShop.getShopName();
        String zone = modelShop.getZone();

        loadReviews(modelShop,holder);//load avg ratings,set to rating bar
        //set data
        holder.shopNameTv.setText(shopName);
        holder.phoneTv.setText(phone);
        holder.addressTv.setText(address);

        //check if online
        if (online.equals("true")){
            //Shop owner is online
            holder.shopClosedTv.setVisibility(View.VISIBLE);
        }else{
            //Shop owner is offline
            holder.shopClosedTv.setVisibility(View.GONE);
        }

        //check if Shop is open
        if (shopOpen.equals("true")){
            //Shop open
            holder.onlineIv.setVisibility(View.VISIBLE);
        }else{
            //Shop closed
            holder.onlineIv.setVisibility(View.GONE);
        }

        try {
            Picasso.get().load(profileImage).placeholder(R.drawable.ic_store_24).into(holder.shopIv);
        }catch (Exception e){
            holder.shopIv.setImageResource(R.drawable.ic_store_24);
        }

        //Handle click listener, shows shop details
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                Intent intent = new Intent(context, ShopDetailsActivity.class);
                intent.putExtra("shopUid",uid);
                context.startActivity(intent);
            }
        });
    }

    private float ratingSum = 0;
    private void loadReviews(ModelShop modelShop, final HolderShop holder) {
        String shopUid  = modelShop.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Ratings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ratingSum = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            float rating = Float.parseFloat("" + ds.child("ratings").getValue());//e.g 4.3
                            ratingSum = ratingSum + rating;//for avg rating, add (addition of) all ratings later we will divide by number of reviews
                        }
                        long numberOfReviews = snapshot.getChildrenCount();
                        float avgRatings = ratingSum / numberOfReviews;
                        holder.ratingBar.setRating(avgRatings);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    @Override
    public int getItemCount() {
        return shopsList.size();
    }

    //View Holder
    class HolderShop extends RecyclerView.ViewHolder{

        //UI view of row_shop.xml
        private ImageView shopIv,onlineIv;
        private TextView shopClosedTv, shopNameTv,phoneTv, addressTv;
        private RatingBar ratingBar;

        public HolderShop(@NonNull View itemView) {
            super(itemView);

            //init UI
            shopIv = itemView.findViewById(R.id.shopIv);
            onlineIv = itemView.findViewById(R.id.onlineIv);
            shopClosedTv = itemView.findViewById(R.id.shopClosedTv);
            shopNameTv = itemView.findViewById(R.id.shopNameTv);
            phoneTv = itemView.findViewById(R.id.phoneTv);
            addressTv = itemView.findViewById(R.id.addressTv);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
