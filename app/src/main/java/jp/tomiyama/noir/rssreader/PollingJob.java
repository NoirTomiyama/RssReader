package jp.tomiyama.noir.rssreader;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


// 定期的にRSSに更新がないかチェックするジョブサービス

public class PollingJob extends JobService {

    private final static String CHANNEL_ID = "update_channel";
    private final static int REQUEST_CODE = 1;

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d("onStartJob","onStartJob");
//        Toast.makeText(getApplicationContext(),"onStartJob",Toast.LENGTH_SHORT).show();

        // 通信をするため、非同期的に実行する
        new Thread((new Runnable() {
            public final void run() {

                // rssのダウンロード
                InputStream response = httpGet("https://www.sbbit.jp/rss/HotTopics.rss");
                if (response != null) {
                    // RSSオブジェクトにパースする
                    Rss rss = parseRss(response);

                    // プリファレンス
                    SharedPreferences prefs = getSharedPreferences("pref_polling", Context.MODE_PRIVATE);
                    long lastFetchTime = prefs.getLong("last_publish_time", 0L);

//                    notifyUpdate(PollingJob.this);

                    // 前回更新されたときの時間．保存されていない場合は0を返す
                    if (lastFetchTime > 0L && lastFetchTime < rss.getPubDate().getTime()) {
                        // 通知する
                        notifyUpdate(PollingJob.this);
                    }

                    // 取得時間を保存する
                    prefs.edit().putLong("last_publish_time", rss.getPubDate().getTime()).apply();
                }
            }
        })).start();

        return true;
    }

    private InputStream httpGet(String url){

        try {
            // 通信接続用のオブジェクトを作る
            URLConnection urlConnection = new URL(url).openConnection();
            HttpsURLConnection con = (HttpsURLConnection) urlConnection;

            // 接続の設定を行う
            con.setRequestMethod("GET");            // メソッド
            con.setConnectTimeout(3000);            // 接続のタイムアウト(ミリ秒)
            con.setReadTimeout(5000);               // 読む込みのタイムアウト(ミリ秒)
            con.setInstanceFollowRedirects(true);   // リダイレクト許可

            // 接続する
            con.connect();

            int responseCode = con.getResponseCode();

            // ステータスコードの確認
            if(200 <= responseCode && responseCode <= 299){
                // 成功したら、レスポンスの入力ストリームを、BufferedInputStreamとして返す
                return new BufferedInputStream(con.getInputStream());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 失敗
        return null;
    }

    // RSS2.0をパースしてRssオブジェクトに変換する
    private Rss parseRss(InputStream stream){

        try {
            // XMLをDOMオブジェクトに変換する
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            stream.close();

            // XPathを生成する
            XPath xPath = XPathFactory.newInstance().newXPath();

            // RSS2.0の日付書式である、RFC1123をDate型に変換するためのオブジェクト
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

            // チャンネル内の<item>要素を全て取り出す
            NodeList items = (NodeList)xPath.evaluate("/rss/channel//item", doc, XPathConstants.NODESET);

            // RSSフィード内の記事の一覧
            ArrayList<Article> articles = new ArrayList<>();

            // <item>の要素ごとに繰り返す
            for(int i = 0; i < items.getLength(); i++){
                Node item = items.item(i);

                // Articleオブジェクトにまとめる
                Article article = new Article(
                        xPath.evaluate("./title/text()", item),
                        xPath.evaluate("./link/text()", item),
                        formatter.parse(xPath.evaluate("./pubDate/text()", item)));

                articles.add(article);
            }

            // RSSオブジェクトにまとめて返す
            return new Rss(xPath.evaluate(
                    "/rss/channel/title/text()", doc),
                    formatter.parse(xPath.evaluate("/rss/channel/pubDate/text()", doc)),
                    articles);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (ParseException e){
            e.printStackTrace();
        }

        return null;
    }

    // 更新通知を行う
    public void notifyUpdate(Context context){
        // 通知をタップした時に起動する画面
        Intent intent = new Intent(context,MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        https://qiita.com/naoty_k/items/f733ff8a69a141331bba

        // 通知に設定するために、PendingIntentにする
        PendingIntent pendingIntent = PendingIntent.getActivity(context,REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
        // 通知を作成する
        Notification notification = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setContentTitle("記事が更新されました")          // 通知のタイトル
                .setContentText("新しい記事をチェックしましょう")  // 通知のテキスト
                .setContentIntent(pendingIntent) // 通知タップ時に起動するIntent
                .setSmallIcon(R.drawable.ic_notification)      // 通知のアイコン
                .setAutoCancel(true) // 通知をタップしたら、その通知を消す
                .build();

        // 通知する
        NotificationManagerCompat.from(context).notify(1, notification);

        Log.d("notifyUpdate","notifyUpdate");

    }


}
