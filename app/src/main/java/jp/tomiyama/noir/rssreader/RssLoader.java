package jp.tomiyama.noir.rssreader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

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


// RSSフィードをダウンロードしてRssオブジェクトを返すローダー
public class RssLoader extends AsyncTaskLoader<Rss> {

    private Rss cache = null;

    public RssLoader(@NonNull Context context) {
        super(context);
    }


    // このローダーがバックグラウンドで行う処理
    @Nullable
    @Override
    public Rss loadInBackground() {

        // HTTPでRSSのXMLを取得する
        InputStream response = httpGet("https://www.sbbit.jp/rss/HotTopics.rss");

        if(response != null){
            // 取得に成功したら、パースして返す
            return parseRss(response);
        }

        return null;
    }

    // コールバッククラスに返す前に通る処理
    @Override
    public void deliverResult(@Nullable Rss data) {
        // 破棄されていたら結果を返さない
        if(isReset() || data == null) {
            return;
        }
        // 結果をキャッシュする
        cache = data;
        super.deliverResult(data);
    }

    // バックグラウンド処理が開始される前に呼ばれる
    @Override
    protected void onStartLoading() {
        // キャッシュがあるなら、キャッシュを返す
        if(cache != null){
            deliverResult(cache);
        }

        // コンテンツが変化している場合やキャッシュがない場合には、バックグラウンド処理を行う
        if(takeContentChanged() || cache == null){
            forceLoad();
        }
    }

    // ローダーが停止する前に呼ばれる処理
    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    // ローダーが破棄される前に呼ばれる処理
    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        cache = null;
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
}
