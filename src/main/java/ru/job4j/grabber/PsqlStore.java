package ru.job4j.grabber;

import ru.job4j.quartz.AlertRabbit;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import static java.sql.Timestamp.valueOf;

public class PsqlStore implements Store, AutoCloseable {
    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            String url = cfg.getProperty("jdbc.url");
            String login = cfg.getProperty("jdbc.username");
            String password = cfg.getProperty("jdbc.password");
            cnn = DriverManager.getConnection(url, login, password);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Properties getProperties() {
        Properties properties = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("post.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                resultSet.getTimestamp("created")
                        .toLocalDateTime()
        );
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = cnn.prepareStatement(
                "insert into post (name, text, link, created) values (?, ?, ?, ?);",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, valueOf(post.getCreated()));
            statement.execute();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    post.setId(resultSet.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try (PreparedStatement statement = cnn.prepareStatement(
                "select * from post;")) {
            try (ResultSet resultSet =  statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(getPost(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Post findById(int id) {
        try (PreparedStatement statement = cnn.prepareStatement(
                "select * from post where id = ?;")) {
            statement.setInt(1, id);
            try (ResultSet resultSet =  statement.executeQuery()) {
                if (resultSet.next()) {
                    return getPost(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        Properties properties = getProperties();
        try (PsqlStore store = new PsqlStore(properties)) {
            LocalDateTime ldt1 = LocalDateTime.now();
            LocalDateTime ldt2 = ldt1.plusMinutes(7);
            LocalDateTime ldt3 = ldt2.plusMinutes(7);
            store.save(new Post("vacancy1", "link1", "description1", ldt1));
            store.save(new Post("vacancy2", "link2", "description2", ldt2));
            store.save(new Post("vacancy3", "link3", "description3", ldt3));
            List<Post> list = store.getAll();
            for (Post vacancy : list) {
                System.out.println(vacancy);
            }
            Post foundById = store.findById(3);
            System.out.println(foundById);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
