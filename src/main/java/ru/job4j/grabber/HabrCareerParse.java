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

    private static final int COUNT_PAGES = 5;

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
            for (int page = 1; page <= COUNT_PAGES; page++) {
                String currentPage = String.format("%s%d", link, page);
                Connection connection = Jsoup.connect(currentPage);
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> list.add(getPost(row)));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Something went wrong");
        }
        return list;
    }

    private Post getPost(Element element) {
        Element titleElement = element.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element dateElement = element.select(".vacancy-card__date").first();
        Element dateLink = dateElement.child(0);
        String vacancyName = titleElement.text();
        String postLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        String dateTime = String.format("%s", dateLink.attr("datetime"));
        String description = retrieveDescription(postLink);
        LocalDateTime localDateTime = dateTimeParser.parse(dateTime);
        return new Post(vacancyName, postLink, description, localDateTime);
    }

    private String retrieveDescription(String link) {
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            Element description = document.select(".collapsible-description__content").first();
            return description.text();
        } catch (IOException e) {
            throw new IllegalArgumentException("Vacancy doesn't have description");
        }
    }
}
