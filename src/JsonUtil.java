import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JsonUtil.java
 * Simple, lightweight JSON serializer and parser.
 * Built entirely with standard Core Java (no external libraries needed).
 * Perfect for 2nd-year B.Tech academic explanation.
 */
public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String patientToJson(Patient p) {
        if (p == null) return "null";
        int visits = (p.getVisitHistory() != null) ? p.getVisitHistory().size() : 0;
        return "{" +
                "\"id\":\"" + escape(p.getId()) + "\"," +
                "\"name\":\"" + escape(p.getName()) + "\"," +
                "\"age\":" + p.getAge() + "," +
                "\"gender\":\"" + escape(p.getGender()) + "\"," +
                "\"phone\":\"" + escape(p.getPhone()) + "\"," +
                "\"visitCount\":" + visits +
                "}";
    }

    public static String appointmentToJson(Appointment a) {
        if (a == null) return "null";
        return "{" +
                "\"appointmentId\":\"" + escape(a.getAppointmentId()) + "\"," +
                "\"patientId\":\"" + escape(a.getPatientId()) + "\"," +
                "\"patientName\":\"" + escape(a.getPatientName()) + "\"," +
                "\"tokenNumber\":" + a.getTokenNumber() + "," +
                "\"appointmentDate\":\"" + escape(a.getAppointmentDate()) + "\"," +
                "\"appointmentTime\":\"" + escape(a.getAppointmentTime()) + "\"," +
                "\"reason\":\"" + escape(a.getReason()) + "\"," +
                "\"status\":\"" + escape(a.getStatus()) + "\"," +
                "\"waitingTimeMinutes\":" + a.getWaitingTimeMinutes() + "," +
                "\"diagnosis\":\"" + escape(a.getDiagnosis()) + "\"," +
                "\"prescription\":\"" + escape(a.getPrescription()) + "\"" +
                "}";
    }

    public static String visitRecordToJson(VisitRecord v) {
        if (v == null) return "null";
        return "{" +
                "\"recordId\":\"" + escape(v.getRecordId()) + "\"," +
                "\"appointmentId\":\"" + escape(v.getAppointmentId()) + "\"," +
                "\"patientId\":\"" + escape(v.getPatientId()) + "\"," +
                "\"visitDate\":\"" + escape(v.getVisitDate()) + "\"," +
                "\"diagnosis\":\"" + escape(v.getDiagnosis()) + "\"," +
                "\"prescription\":\"" + escape(v.getPrescription()) + "\"," +
                "\"waitingTimeMinutes\":" + v.getWaitingTimeMinutes() +
                "}";
    }

    public static <T> String listToJson(List<T> list, Function<T, String> mapper) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapper.apply(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parses a simple flat JSON string into key-value map.
     * E.g. {"name": "John", "age": 25} -> map with "name"="John", "age"="25"
     */
    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        // Simple tokenization by commas not inside quotes
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        List<String> pairs = new java.util.ArrayList<>();

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (c == ',' && !inQuote) {
                pairs.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }

        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                String key = cleanToken(pair.substring(0, colonIdx));
                String value = cleanToken(pair.substring(colonIdx + 1));
                map.put(key, value);
            }
        }
        return map;
    }

    private static String cleanToken(String str) {
        if (str == null) return "";
        String s = str.trim();
        // Remove surrounding quotes or escaped quotes
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        } else if (s.startsWith("\\\"") && s.endsWith("\\\"") && s.length() >= 4) {
            s = s.substring(2, s.length() - 2);
        }
        // Unescape internal escaped characters
        s = s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
        return s.trim();
    }
}
