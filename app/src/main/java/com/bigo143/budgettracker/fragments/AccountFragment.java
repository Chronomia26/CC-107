package com.bigo143.budgettracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

    private ArrayList<CategoryModel> list = new ArrayList<>();
    private RecyclerView rv;
    private CategoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private String currentUser;

    // ✅ FIXED: Remove parameterized constructor
    public AccountFragment() {
        // Required empty public constructor
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

        // ✅ Load data here instead of constructor
        loadData();

        adapter = new CategoryAdapter(list, CategoryAdapter.TYPE_ACCOUNT);

        // ✅ Set the action listener
        adapter.setOnCategoryActionListener(new CategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onDeleteCategory(String categoryName, int categoryId) {
                Fragment parent = getParentFragment();
                if (parent instanceof CategoriesFragment) {
                    ((CategoriesFragment) parent).deleteCategoryDialog(categoryId, categoryName);
                }
            }

            @Override
            public void onEditCategory(String categoryName, int categoryId) {
                Fragment parent = getParentFragment();
                if (parent instanceof CategoriesFragment) {
                    CategoryModel cat = dbHelper.getCategoryById(categoryId, currentUser);
                    ((CategoriesFragment) parent).showEditCategoryDialog(categoryId, cat.getName(), cat.getIcon());
                }
            }
        });

        rv.setAdapter(adapter);

        return v;
    }

    // ✅ NEW: Load data method
    private void loadData() {
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
    }

    public void addCategory(CategoryModel category){
        list.add(category);
        adapter.notifyItemInserted(list.size() - 1);
    }

    public void updateList(ArrayList<CategoryModel> newList) {
        this.list = newList;
        adapter.updateData(newList);
    }

    public void reloadData() {
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(requireContext());
        }

        if (currentUser == null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            currentUser = prefs.getString("logged_in_user", null);
            if (currentUser == null) return;
        }

        loadData();

        if(adapter != null)
            adapter.notifyDataSetChanged();
    }
}