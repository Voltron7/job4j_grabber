package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) {
        HabrCareerParse parser = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> list = parser.list(PAGE_LINK);
        for (Post vacancy : list) {
            System.out.println(vacancy);
        }
    }

    @Override
    public List<Post> list(String link) {
        List<Post> list = new ArrayList<>();
        try {
            for (int page = 1; page <= 5; page++) {
                String currentPage = String.format("%s%d", link, page);
                Connection connection = Jsoup.connect(currentPage);
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> {
                    Element titleElement = row.select(".vacancy-card__title").first();
                    Element linkElement = titleElement.child(0);
                    Element dateElement = row.select(".vacancy-card__date").first();
                    Element dateLink = dateElement.child(0);
                    String vacancyName = titleElement.text();
                    String postLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                    String dateTime = String.format("%s", dateLink.attr("datetime"));
                    String description = retrieveDescription(postLink);
                    HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();
                    LocalDateTime localDateTime = parser.parse(dateTime);
                    list.add(new Post(vacancyName, postLink, description, localDateTime));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String retrieveDescription(String link) {
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            Element description = document.select(".collapsible-description__content").first();
            return description.text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Description doesn't exist";
    }
}
