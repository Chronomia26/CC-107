package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.adapters.CategoryAdapter;
import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class AccountFragment extends Fragment {

    private ArrayList<CategoryModel> list;
    private RecyclerView rv;
    private CategoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private String currentUser;

    public AccountFragment(ArrayList<CategoryModel> list){
        this.list = list;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbHelper = new DatabaseHelper(context);

        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);
        if (currentUser == null) {
            throw new IllegalStateException("No logged in user found in SharedPreferences");
        }

    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){

        View v = inflater.inflate(R.layout.fragment_child_list, container, false);



        rv = v.findViewById(R.id.recyclerViewChild);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CategoryAdapter(list, CategoryAdapter.TYPE_ACCOUNT);
        rv.setAdapter(adapter);

        return v;
    }

    // Add new category from the Add Dialog
    public void addCategory(CategoryModel category){
        list.add(category);
        adapter.notifyItemInserted(list.size() - 1);
    }

    // 🔥 Called when balance changes or records change
    public void updateList(ArrayList<CategoryModel> newList) {
        this.list = newList;            // update fragment's list
        adapter.updateData(newList);     // update adapter
    }
    // 🔹 Reload account balances dynamically
    public void reloadData() {
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(requireContext());
        }

        if (currentUser == null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            currentUser = prefs.getString("logged_in_user", null);
            if (currentUser == null) return; // prevent crash if no logged-in user
        }

        list.clear();

        Cursor cursor = dbHelper.getCategoriesByType(currentUser, "account");
        if(cursor != null && cursor.moveToFirst()){
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow("icon"));
                double balance = dbHelper.getAccountBalance(id, currentUser);
                String subtitle = "Balance: ₱ " + String.format("%.2f", balance);
                list.add(new CategoryModel(name, icon, subtitle, balance));
            } while(cursor.moveToNext());
            cursor.close();
        }

        if(adapter != null)
            adapter.notifyDataSetChanged();
        requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

    }

}
