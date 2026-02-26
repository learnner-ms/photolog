package com.example.photolog_front;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FamilyDiaryAdapter extends RecyclerView.Adapter<FamilyDiaryAdapter.ViewHolder> {

    private List<FamilyDiary> diaryList;
    private Context context;
    private OnDiaryClickListener listener;

    public interface OnDiaryClickListener {
        void onDiaryClick(FamilyDiary diary);
    }

    public FamilyDiaryAdapter(Context context, List<FamilyDiary> diaryList, OnDiaryClickListener listener) {
        this.context = context;
        this.diaryList = diaryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_family_diary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyDiary diary = diaryList.get(position);
        holder.tvAuthor.setText(diary.getAuthor());
        holder.tvTitle.setText(diary.getTitle());
        holder.tvContent.setText(diary.getContent());
        holder.tvDate.setText(diary.getDate());
        holder.imgThumbnail.setImageResource(diary.getImageResId());

        holder.itemView.setOnClickListener(v -> listener.onDiaryClick(diary));
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvTitle, tvContent, tvDate;
        ImageView imgThumbnail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvDate = itemView.findViewById(R.id.tv_date);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
        }
    }
}
