package jp.tomiyama.noir.rssreader;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ArticlesAdapter.OnArticleClickListener, LoaderManager.LoaderCallbacks<Rss> {

    private final static String CHANNEL_ID = "update_channel";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        getSupportLoaderManager().initLoader(1,null,this);
//        getLoaderManager().initLoader(1,null);
        // ローダーを呼び出す
        LoaderManager.getInstance(this).initLoader(1,null,this);

        // 通知チャンネルを作成する
        createChannel(this);

        // 定期的に新しい記事が無いかをチェックするジョブ
        JobInfo fetchJob = new JobInfo.Builder(1, new ComponentName(this, PollingJob.class))
                .setPeriodic(TimeUnit.HOURS.toMillis(6)) // 6時間ごとに実行
                .setPersisted(true) // 端末を再起動しても有効
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // ネットワーク接続されていること
                .build();

        // ジョブの登録
        getSystemService(JobScheduler.class).schedule(fetchJob);

    }

    // ArticlesAdapter.OnArticleClickListenerの実装
    @Override
    public void onArticleClick(Article article) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(this, Uri.parse(article.link));
    }

    // 通知チャンネルを作成する
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannel(Context context){

        // 通知チャンネルを作成する
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, // チャンネルID
                "新着記事", // チャンネル名
                NotificationManager.IMPORTANCE_DEFAULT); // チャンネルの重要度
        channel.enableLights(false); // LEDを光らせるか
        channel.enableVibration(true); // バイブレーションを行うか
        channel.setShowBadge(true); // アイコンにバッジをつけるか

        // 端末にチャンネルを登録する
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    // ローダーが要求された時に呼ばれる
    @NonNull
    @Override
    public Loader<Rss> onCreateLoader(int id, @Nullable Bundle bundle) {
        return new RssLoader(this);
    }

    // ローダーで行なった処理が終了したときに呼ばれる
    @Override
    public void onLoadFinished(@NonNull Loader<Rss> loader, Rss data) {
        if(data != null){
            RecyclerView recyclerView = findViewById(R.id.articles);

            ArticlesAdapter adapter = new ArticlesAdapter(this, data.articles);
            adapter.setOnArticleClickListener(this);
            recyclerView.setAdapter(adapter);

            GridLayoutManager layoutManager = new GridLayoutManager(this,2);

            recyclerView.setLayoutManager(layoutManager);
        }
    }

    // ローダーがリセットされた時に呼ばれる
    @Override
    public void onLoaderReset(@NonNull Loader<Rss> loader) {
        // 特になにもしない
    }
}
