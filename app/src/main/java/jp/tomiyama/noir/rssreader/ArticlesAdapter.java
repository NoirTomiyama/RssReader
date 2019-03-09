package jp.tomiyama.noir.rssreader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder> {

    private Context context;
    private ArrayList<Article> articles;
    private OnArticleClickListener onArticleClickListener = null;

    interface OnArticleClickListener{
        void onArticleClick(Article article);
    }

    public void setOnArticleClickListener(OnArticleClickListener onArticleClickListener) {
        this.onArticleClickListener = onArticleClickListener;
    }

    // 新しくViewを作る
    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Viewの生成
        View view = LayoutInflater.from(context).inflate(R.layout.grid_article_cell, parent, false);
        // ViewHolderを生成する
        final ArticleViewHolder viewHolder = new ArticleViewHolder(view);

        // Viewのタップ時の処理
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // タップされた記事の位置
                int position = viewHolder.getAdapterPosition();
                // タップされた位置に応じた記事
                Article article = articles.get(position);
                // コールバックを呼ぶ
                onArticleClickListener.onArticleClick(article);
            }
        });

        return viewHolder;
    }

    // Viewに表示すべき値を設定する
    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        // アダプター中の位置に応じた記事を得る
        Article article = articles.get(position);
        // 記事のタイトルを設定する
        holder.title.setText(article.title);
        // 記事の発行日付を設定する
        holder.pubDate.setText(context.getString(R.string.pubDate,article.pubDate));

    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public ArticlesAdapter(Context context, ArrayList<Article> articles) {
        this.context = context;
        this.articles = articles;
    }

    // ビューホルダー
    static class ArticleViewHolder extends RecyclerView.ViewHolder{

        TextView title;
        TextView pubDate;

        public ArticleViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.title);
            pubDate = view.findViewById(R.id.pubDate);
        }
    }

}
