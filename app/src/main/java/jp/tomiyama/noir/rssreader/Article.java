package jp.tomiyama.noir.rssreader;

import java.util.Date;

// RSSの各記事を表すデータクラス
public class Article {

    String title;
    String link;
    Date pubDate;

    public Article(String title, String link, Date pubDate) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
    }
}
