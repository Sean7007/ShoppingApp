package com.desperdartos.shoppingapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.adapters.AdapterOrderUser;
import com.desperdartos.shoppingapp.adapters.AdapterShop;
import com.desperdartos.shoppingapp.commands.MainUserActivityClicks;
import com.desperdartos.shoppingapp.databinding.ActivityMainUserBinding;
import com.desperdartos.shoppingapp.models.ModelOrderUser;
import com.desperdartos.shoppingapp.models.ModelShop;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class MainUserActivity extends AppCompatActivity {
    ActivityMainUserBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private ArrayList<ModelShop> shopList;
    private AdapterShop adapterShop;
    private ArrayList<ModelOrderUser> orderList;
    private AdapterOrderUser adapterOrderUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main_user);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_user);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();
        showShopsUI(); //At first shops UI

        binding.setClickHandle(new MainUserActivityClicks() {
            @Override
            public void logoutUser() {
                //signOut
                makeMeOffline();
            }

            @Override
            public void editUserProfile() {
                //for edit user profile...now open edit activity
                startActivity(new Intent(MainUserActivity.this, ProfileEditUserActivity.class));
            }

            @Override
            public void shopsTabClick() {
                //show shops
                showShopsUI();            }

            @Override
            public void orderTabsClick() {
                //Show order
                showOrdersUI();
                loadOrders();
            }

            @Override
            public void settingsBtnClicks() {
                startActivity(new Intent(MainUserActivity.this,SettingsActivity.class));
            }
        });

    }

    private void loadOrders() {
        orderList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (final DataSnapshot ds : snapshot.getChildren()) {
                    String uid = "" + ds.getRef().getKey();
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Orders");
                    reference.orderByChild("orderBy").equalTo(firebaseAuth.getUid())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (ds.exists()) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            ModelOrderUser modelOrderUser = ds.getValue(ModelOrderUser.class);
                                            //now add to list
                                            orderList.add(modelOrderUser);
                                        }
                                        //setup adapter
                                        adapterOrderUser = new AdapterOrderUser(MainUserActivity.this,orderList);
                                        binding.orderRv.setAdapter(adapterOrderUser);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //At start show shops Ui
    private void showShopsUI() {
        binding.shopsRl.setVisibility(View.VISIBLE);
        binding.ordersRl.setVisibility(View.GONE);

        binding.tabShopsTv.setTextColor(getResources().getColor(R.color.colorBlack));
        binding.tabShopsTv.setBackgroundResource(R.drawable.shape_rec04);

        binding.tabOrdersTv.setTextColor(getResources().getColor(R.color.white));
        binding.tabOrdersTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    // show ORDERS UI
    private void showOrdersUI() {
        binding.shopsRl.setVisibility(View.GONE);
        binding.ordersRl.setVisibility(View.VISIBLE);

        binding.tabOrdersTv.setTextColor(getResources().getColor(R.color.colorBlack));
        binding.tabOrdersTv.setBackgroundResource(R.drawable.shape_rec04);

        binding.tabShopsTv.setTextColor(getResources().getColor(R.color.white));
        binding.tabShopsTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    //Method that signs out user
    private void makeMeOffline() {
        //After logging in, make user online
        progressDialog.setMessage("Logging Out");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("online","false");

        //updates value in db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Update Successful
                        firebaseAuth.signOut();
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed Updating
                        progressDialog.dismiss();
                        Toast.makeText(MainUserActivity.this, "" +e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user == null){
            startActivity(new Intent(MainUserActivity.this, LoginActivity.class));
            finish();
        }else{
            loadMyInfo();
        }
    }
    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //Get user data
                            String name = "" +ds.child("name").getValue();
                            String email = "" +ds.child("email").getValue();
                            String phone = "" +ds.child("phone").getValue();
                            String profileImage = "" +ds.child("profileImage").getValue();
                            String accountType = "" +ds.child("accountType").getValue();
                            String city = "" +ds.child("city").getValue();

                            //Set user data
                            binding.nameTv.setText(name);
                            binding.emailTv.setText(email);
                            binding.phoneTv.setText(phone);
                            try{
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_person_24).into(binding.profileIv);
                            }catch (Exception e){
                                binding.profileIv.setImageResource(R.drawable.ic_person_24);
                            }
                            //load only shops on that are in the city of user
                            loadShops(city);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadShops(final String city) {
        //init list
        shopList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("accountType").equalTo("Seller")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Clear list before adding
                        shopList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelShop modelShop = ds.getValue(ModelShop.class);
                            String shopCity = ""+ds.child("city").getValue();

                            //Show only user city shops
                            if (shopCity.equals(city)){
                                shopList.add(modelShop);
                            }
                            //if you want to display all shops, skip if statement and add this
                            // shopsList.add(modelShop);
                        }
                        //setup adapter
                        adapterShop = new AdapterShop(MainUserActivity.this, shopList);
                        //set adapter to recyclerView
                        binding.shopsRv.setAdapter(adapterShop);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}