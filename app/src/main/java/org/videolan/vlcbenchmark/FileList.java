package org.videolan.vlcbenchmark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FileList extends AppCompatActivity {
    private final String TAG = "FileList";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.file_list);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        List<String> files = intent.getStringArrayListExtra("files");

        FileListAdapter adapter = new FileListAdapter(files);
        recyclerView.setAdapter(adapter);
    }

    public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
        List<String> mData;

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mName;

            public ViewHolder(@NonNull View view) {
                super(view);
                mName = (TextView)view.findViewById(R.id.file_name);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        TextView textView = (TextView)findViewById(R.id.file_name);
                        Log.i(TAG, "position: " + position + ", file: " + textView.getText());
                        Intent intent = new Intent();
                        intent.putExtra("index", position);
                        FileList.this.setResult(Constants.RequestCodes.SELECTED_FILE, intent);
                        finish();
                    }
                });
            }
        }

        public  FileListAdapter(List<String> files) {
            mData = files;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = mData.get(position);
            holder.mName.setText(name);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
