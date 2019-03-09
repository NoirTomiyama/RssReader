package jp.tomiyama.noir.rssreader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// RSSを表現するデータクラス
public class Rss {

    String title;
    Date pubDate;
    ArrayList<Article> articles;

    public Rss(String title, Date pubDate, ArrayList<Article> articles) {
        this.title = title;
        this.pubDate = pubDate;
        this.articles = articles;
    }


    public Date getPubDate() {
        return pubDate;
    }

}
