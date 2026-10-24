import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class Main {
    private static final StudentManager manager = new StudentManager();

    public static void main(String[] args) throws IOException {
        manager.addStudent(new Student("101", "Arun", "BCA", "arun@example.com"));
        manager.addStudent(new Student("102", "Meera", "BCA", "meera@example.com"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", Main::serveStatic);
        server.createContext("/api/login", Main::login);
        server.createContext("/api/students", Main::students);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Student Management System is running.");
        System.out.println("Open http://localhost:8080 in your browser.");
        System.out.println("Demo login: admin  |  password: admin123");
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        String safe = path.replace("..", "");
        File file = new File("public" + safe);
        if (!file.exists() || file.isDirectory()) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        String type = safe.endsWith(".html") ? "text/html; charset=utf-8"
                : safe.endsWith(".css") ? "text/css; charset=utf-8"
                : safe.endsWith(".js") ? "application/javascript; charset=utf-8"
                : "application/octet-stream";
        send(ex, 200, type, new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    private static void login(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(ex, 405, "{\"success\":false,\"message\":\"Use POST.\"}");
            return;
        }
        Map<String, String> data = readForm(ex);
        boolean ok = "admin".equals(data.get("userId")) && "admin123".equals(data.get("password"));
        sendJson(ex, ok ? 200 : 401, "{\"success\":" + ok + ",\"message\":\""
                + (ok ? "Login successful." : "Invalid user ID or password.") + "\"}");
    }

    private static void students(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
        Map<String, String> data = method.equals("GET") ? queryParams(ex) : readForm(ex);
        String action = data.getOrDefault("action", "all");

        try {
            switch (action) {
                case "add": {
                    Student s = new Student(required(data, "id"), required(data, "name"),
                            required(data, "course"), required(data, "email"));
                    if (manager.findStudent(s.getId()) != null) {
                        sendJson(ex, 409, result(false, "A student with this ID already exists.", null));
                        return;
                    }
                    manager.addStudent(s);
                    sendJson(ex, 200, result(true, "Student added successfully.", s));
                    break;
                }
                case "view":
                case "search": {
                    Student s = manager.findStudent(required(data, "id"));
                    sendJson(ex, s == null ? 404 : 200,
                            result(s != null, s == null ? "No student found with that ID." : "Student found.", s));
                    break;
                }
                case "update": {
                    Student s = manager.findStudent(required(data, "id"));
                    if (s == null) {
                        sendJson(ex, 404, result(false, "Student not found; nothing was updated.", null));
                        return;
                    }
                    s.setName(required(data, "name"));
                    s.setCourse(required(data, "course"));
                    s.setEmail(required(data, "email"));
                    sendJson(ex, 200, result(true, "Student updated successfully.", s));
                    break;
                }
                case "delete": {
                    Student removed = manager.deleteStudent(required(data, "id"));
                    sendJson(ex, removed == null ? 404 : 200,
                            result(removed != null, removed == null ? "Student not found." : "Student deleted successfully.", removed));
                    break;
                }
                case "all": {
                    sendJson(ex, 200, allResult(manager.getAllStudents()));
                    break;
                }
                default:
                    sendJson(ex, 400, result(false, "Unknown operation.", null));
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, result(false, e.getMessage(), null));
        }
    }

    private static String required(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Please enter " + key + ".");
        return value.trim();
    }

    private static String result(boolean success, String message, Student s) {
        return "{\"success\":" + success + ",\"message\":\"" + esc(message) + "\",\"student\":"
                + (s == null ? "null" : s.toJson()) + "}";
    }

    private static String allResult(List<Student> students) {
        StringBuilder b = new StringBuilder("{\"success\":true,\"message\":\"All student records displayed.\",\"students\":[");
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) b.append(",");
            b.append(students.get(i).toJson());
        }
        return b.append("]}").toString();
    }

    private static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseParams(body);
    }

    private static Map<String, String> queryParams(HttpExchange ex) {
        return parseParams(ex.getRequestURI().getRawQuery() == null ? "" : ex.getRequestURI().getRawQuery());
    }

    private static Map<String, String> parseParams(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        send(ex, status, "application/json; charset=utf-8", body);
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}

class Student {
    private final String id;
    private String name;
    private String course;
    private String email;

    public Student(String id, String name, String course, String email) {
        this.id = id;
        this.name = name;
        this.course = course;
        this.email = email;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCourse() { return course; }
    public String getEmail() { return email; }
    public void setName(String name) { this.name = name; }
    public void setCourse(String course) { this.course = course; }
    public void setEmail(String email) { this.email = email; }

    public String toJson() {
        return "{\"id\":\"" + MainJson.escape(id) + "\",\"name\":\"" + MainJson.escape(name)
                + "\",\"course\":\"" + MainJson.escape(course) + "\",\"email\":\"" + MainJson.escape(email) + "\"}";
    }
}

class StudentManager {
    private final Map<String, Student> students = new LinkedHashMap<>();
    public void addStudent(Student student) { students.put(student.getId(), student); }
    public Student findStudent(String id) { return students.get(id); }
    public Student deleteStudent(String id) { return students.remove(id); }
    public List<Student> getAllStudents() { return new ArrayList<>(students.values()); }
}

class MainJson {
    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }
}
