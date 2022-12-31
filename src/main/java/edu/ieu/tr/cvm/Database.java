package edu.ieu.tr.cvm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Database {
    private static Database instance;

    public static Database getInstance() throws ClassNotFoundException, SQLException {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    private Connection connection;

    private Database() {
    }

    public void open() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:cvm.db");
        final Statement statement = connection.createStatement();
        final String string = "create table if not exists cv " +
                "(id integer primary key autoincrement," +
                "fullName text," +
                "birthYear integer," +
                "email text," +
                "description text," +
                "homeAddress text," +
                "jobAddress text," +
                "phone text," +
                "website text); " +

                "create table if not exists skill " +
                "(id integer primary key autoincrement," +
                "cv_id integer," +
                "name text," +
                "level integer," +
                "foreign key(cv_id) references cv(id) on update cascade on delete cascade); " +

                "create table if not exists education " +
                "(id integer primary key autoincrement," +
                "cv_id integer," +
                "name text," +
                "foreign key(cv_id) references cv(id) on update cascade on delete cascade); " +

                "create table if not exists additional_skill " +
                "(id integer primary key autoincrement," +
                "cv_id integer," +
                "name text," +
                "level integer," +
                "foreign key(cv_id) references cv(id) on update cascade on delete cascade);" +

                "create table if not exists tag " +
                "(id integer primary key autoincrement," +
                "cv_id integer," +
                "name text," +
                "foreign key(cv_id) references cv(id) on update cascade on delete cascade)";
        statement.executeUpdate(string);
        statement.close();
    }

    private List<Cv> toObject(String query) throws SQLException {
        final Map<Integer, Cv> cvs = new HashMap<>();
        final PreparedStatement statement1 = connection.prepareStatement(query);
        final ResultSet set1 = statement1.executeQuery();
        while (set1.next()) {
            cvs.put(set1.getInt(1), new Cv(set1.getInt(1),
                    set1.getString(2),
                    set1.getInt(3),
                    set1.getString(4),
                    set1.getString(5),
                    set1.getString(6),
                    set1.getString(7),
                    set1.getString(8),
                    set1.getString(9),
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new ArrayList<>()));
        }
        statement1.close();

        final PreparedStatement statement2 = connection.prepareStatement("select * from skill");
        final ResultSet set2 = statement2.executeQuery();

        while (set2.next()) {
            final Map<String, Integer> skills = new HashMap<>();
            skills.put(set2.getString(3), set2.getInt(4));
            cvs.get(set2.getInt(2)).setSkills(skills);
        }
        statement2.close();

        final PreparedStatement statement3 = connection.prepareStatement("select * from additional_skill");
        final ResultSet set3 = statement3.executeQuery();

        while (set3.next()) {
            final Map<String, Integer> additionalSkills = new HashMap<>();
            additionalSkills.put(set3.getString(3), set3.getInt(4));
            cvs.get(set3.getInt(2)).setAdditionalSkills(additionalSkills);
        }
        statement3.close();

        final PreparedStatement statement5 = connection.prepareStatement("select * from education");
        final ResultSet set5 = statement5.executeQuery();

        while (set5.next()) {
            final Map<String, Integer> education = new HashMap<>();
            education.put(set5.getString(3), set5.getInt(4));
            cvs.get(set5.getInt(2)).setSkills(education);
        }
        statement5.close();

        final PreparedStatement statement4 = connection.prepareStatement("select * from tag");
        final ResultSet set4 = statement4.executeQuery();

        while (set4.next()) {
            final List<String> tags = new ArrayList<>();
            tags.add(set4.getString(3));
            cvs.get(set4.getInt(2)).setTags(tags);
        }
        statement4.close();
        new ArrayList<>(cvs.values()).forEach(cv -> cv.print());
        return new ArrayList<>(cvs.values());
    }

    public List<Cv> getAll() throws SQLException {
        return toObject("select * from cv");
    }

    public List<Cv> filter(String fieldName, String fieldValue) throws SQLException {
        return toObject(String.format("select * from cv where %s like '%%%s%%'", fieldName, fieldValue));
    }

    public String educationToString(final Cv cv) {
        String s = "";

        for (final Map.Entry<String, Integer> entry : cv.getEducation().entrySet()) {
            s += "insert into education (cv_id,name) values (" + cv.getId() + ",'" + entry.getKey() + "');";

        }
        return s;
    }

    public String tagToString(final Cv cv) {
        final StringBuilder sb = new StringBuilder();
        cv.getTags().forEach(e -> {
            sb.append("insert into tag (cv_id,name) values (" + cv.getId() + "," + e + ");");
        });
        return sb.toString();
    }

    public String skillToString(final Cv cv) {
        String s = "";
        for (final Map.Entry<String, Integer> entry : cv.getSkills().entrySet())
            s += "insert into skill (cv_id,level) values (" + cv.getId() + "," + entry.getKey() + ");";
        return s;
    }

    public String additionalskillToString(final Cv cv) {
        String s = "";
        for (final Map.Entry<String, Integer> entry : cv.getAdditionalSkills().entrySet())
            s += "insert into additional_skill (cv_id,level) values (" + cv.getId() + "," + entry.getKey() + ");";

        return s;
    }

    public Cv insert(final Cv cv) throws SQLException {
        final PreparedStatement statement1 = connection.prepareStatement("insert into cv " +
                "(fullName, " +
                "birthYear, " +
                "email, " +
                "description, " +
                "homeAddress, " +
                "jobAddress, " +
                "phone, " +
                "website) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?) " +
                "returning id");
        statement1.setString(1, cv.getFullName());
        statement1.setInt(2, cv.getBirthYear());
        statement1.setString(3, cv.getEmail());
        statement1.setString(4, cv.getDescription());
        statement1.setString(5, cv.getHomeAddress());
        statement1.setString(6, cv.getJobAddress());
        statement1.setString(7, cv.getPhone());
        statement1.setString(8, cv.getWebsite());
        final ResultSet set = statement1.executeQuery();
        cv.setId(set.getInt(1));
        statement1.close();
        final Statement statement2 = connection.createStatement();
        statement2.executeUpdate(
                skillToString(cv) + educationToString(cv) + additionalskillToString(cv) + tagToString(cv));
        statement2.close();
        return cv;
    }

    public void delete(final Cv cv) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("delete from cv where id = ?");

        statement.setInt(1, cv.getId());
        statement.executeUpdate();
        statement.close();
    }

    public void close() throws SQLException {
        connection.close();
    }
}
