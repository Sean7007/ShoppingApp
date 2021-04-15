package com.desperdartos.shoppingapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.desperdartos.shoppingapp.Constants;
import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.adapters.AdapterOrderShop;
import com.desperdartos.shoppingapp.commands.MainSellerActivityClicks;
import com.desperdartos.shoppingapp.databinding.ActivityMainSellerBinding;
import com.desperdartos.shoppingapp.models.ModelOrderShop;
import com.desperdartos.shoppingapp.models.ModelProduct;
import com.desperdartos.shoppingapp.adapters.AdapterProductSeller;
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

public class MainSellerActivity extends AppCompatActivity {
    ActivityMainSellerBinding binding;
    FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;
    private ArrayList<ModelProduct> productList;
    private ArrayList<ModelOrderShop> orderShopArrayList;
    private AdapterProductSeller adapterProductSeller;
    private AdapterOrderShop adapterOrderShop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main_seller);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_seller);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();
        loadAllProducts();
        showProductsUI();
        loadAllOrders();

        //search
        binding.searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    adapterProductSeller.getFilter().filter(charSequence);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.setClickHandle(new MainSellerActivityClicks() {
            @Override
            public void logoutSeller() {
                //make offline
                //and go to login activity
                makeMeOffline();
            }

            @Override
            public void editSellerProfile() {
                //for edit seller profile...now open edit activity
                startActivity(new Intent(MainSellerActivity.this, ProfileEditSellerActivity.class));
            }

            @Override
            public void addProductBtn() {
                //for add product...open add product activity.
                startActivity(new Intent(MainSellerActivity.this, AddProductActivity.class));
            }

            @Override
            public void tabProductsClicks() {
                //load products
                showProductsUI();
            }

            @Override
            public void tabOrdersClicks() {
                //load orders
                showOrdersUI();
            }

            @Override
            public void filterProductBtnClick() {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainSellerActivity.this);
                builder.setTitle("Choose Category").setItems(Constants.PRODUCT_CATEGORY1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selected = Constants.PRODUCT_CATEGORY1[which];
                        binding.filteredProductsTv.setText(selected);
                        if (selected.equals("All")) {
                            loadAllProducts();
                        } else {
                            //load filtered product
                            loadFilteredProducts(selected);
                        }
                    }
                }).create().show();

            }

            @Override
            public void filterOrderBtnclick() {
                final String options[] = {"All", "In Progress", "Complete", "Cancelled"};
                //dialog
                androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(MainSellerActivity.this);
                builder.setTitle("Filter Orders :")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    //all
                                    binding.filteredOrdersTv.setText("Showing All");
                                    adapterOrderShop.getFilter().filter("");//show all orders
                                } else {
                                    String optionClicked = options[which];
                                    binding.filteredOrdersTv.setText("Showing " + optionClicked + " Orders");
                                    adapterOrderShop.getFilter().filter(optionClicked);
                                }
                            }
                        });
                builder.create().show();
            }

            @Override
            public void reviewsBtnClick() {
                //open same reviews activity as uses user review acivity
                Intent intent = new Intent(MainSellerActivity.this, ShopReviewsActivity.class);
                intent.putExtra("shopUid", "" + firebaseAuth.getUid());
                startActivity(intent);
            }

            @Override
            public void settingsBtnClicks() {
                startActivity(new Intent(MainSellerActivity.this, SettingsActivity.class));
            }
        });
    }

    private void loadAllOrders() {
        orderShopArrayList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Orders")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderShopArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ModelOrderShop modelOrderShop = ds.getValue(ModelOrderShop.class);
                            orderShopArrayList.add(modelOrderShop);
                        }
                        adapterOrderShop = new AdapterOrderShop(MainSellerActivity.this, orderShopArrayList);
                        binding.ordersRv.setAdapter(adapterOrderShop);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadFilteredProducts(String selected) {
        productList = new ArrayList<>();
        //Get All Products
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Before getting reset list
                        for (DataSnapshot ds: snapshot.getChildren()){

                            String productCategory = "" + ds.child("productCategory").getValue();
                            //if selected category match then add in list
                            if (selected.equals(productCategory)){
                                ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                                productList.add(modelProduct);
                            }
                        }
                        //Setup adapter
                        adapterProductSeller = new AdapterProductSeller(MainSellerActivity.this, productList);
                        //Set adapter
                        binding.productRv.setAdapter(adapterProductSeller);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAllProducts() {
        productList = new ArrayList<>();

        //Get All Products
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Before getting reset list
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                            productList.add(modelProduct);
                        }
                        //Setup adapter
                        adapterProductSeller = new AdapterProductSeller(MainSellerActivity.this, productList);
                        //Set adapter
                        binding.productRv.setAdapter(adapterProductSeller);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showOrdersUI() {
        //Show or hides orders UI
        binding.ordersRl.setVisibility(View.VISIBLE);
        binding.productsRl.setVisibility(View.GONE);

        binding.tabOrdersTv.setTextColor(getResources().getColor(R.color.colorBlack));
        binding.tabOrdersTv.setBackgroundResource(R.drawable.shape_rec04);

        binding.tabProductsTv.setTextColor(getResources().getColor(R.color.white));
        binding.tabProductsTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void showProductsUI() {
        //Show or hides products UI
        binding.productsRl.setVisibility(View.VISIBLE);
        binding.ordersRl.setVisibility(View.GONE);

        binding.tabProductsTv.setTextColor(getResources().getColor(R.color.colorBlack));
        binding.tabProductsTv.setBackgroundResource(R.drawable.shape_rec04);

        binding.tabOrdersTv.setTextColor(getResources().getColor(R.color.white));
        binding.tabOrdersTv.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

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
                        Toast.makeText(MainSellerActivity.this, "" +e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user == null){
            startActivity(new Intent(MainSellerActivity.this, LoginActivity.class));
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
                            //Get Data from db
                            String name = "" +ds.child("name").getValue();
                            String accountType = "" +ds.child("accountType").getValue();
                            String email = "" +ds.child("email").getValue();
                            String shopName = "" +ds.child("shopName").getValue();
                            String profileImage = "" +ds.child("profileImage").getValue();

                            //Set data to UI
                            binding.nameTv.setText(name);
                            binding.shopNameTv.setText(email);
                            binding.emailTv.setText(shopName);

                            try{
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_store_24).into(binding.profileIv);
                            }catch(Exception e){
                                binding.profileIv.setImageResource(R.drawable.ic_store_24);
                            }



                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}