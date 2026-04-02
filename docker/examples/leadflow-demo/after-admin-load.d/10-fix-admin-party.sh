#!/bin/sh
set -eu

cat >/tmp/fix-admin-party.java <<'EOF'
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class fix_admin_party {
    public static void main(String[] args) throws Exception {
        String host = System.getenv("OFBIZ_POSTGRES_HOST");
        String db = System.getenv("OFBIZ_POSTGRES_OFBIZ_DB");
        String user = System.getenv("OFBIZ_POSTGRES_OFBIZ_USER");
        String password = System.getenv("OFBIZ_POSTGRES_OFBIZ_PASSWORD");
        String url = "jdbc:postgresql://" + host + ":5432/" + db;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            stmt.executeUpdate(
                "insert into party (party_id, party_type_id, status_id) " +
                "values ('admin', 'PERSON', 'PARTY_ENABLED') on conflict (party_id) do nothing");
            stmt.executeUpdate(
                "insert into person (party_id, first_name, last_name) " +
                "values ('admin', 'Admin', 'User') on conflict (party_id) do nothing");
            stmt.executeUpdate(
                "insert into party_role (party_id, role_type_id) " +
                "values ('admin', '_NA_') on conflict do nothing");
            stmt.executeUpdate(
                "insert into party_role (party_id, role_type_id) " +
                "values ('admin', 'OWNER') on conflict do nothing");
            stmt.executeUpdate(
                "insert into party_status (party_id, status_id, status_date) " +
                "values ('admin', 'PARTY_ENABLED', current_timestamp) on conflict do nothing");
            stmt.executeUpdate(
                "update user_login set party_id = 'admin', enabled = coalesce(enabled, 'Y') " +
                "where user_login_id = 'admin'");
            conn.commit();
        }
    }
}
EOF

java -cp /ofbiz/lib-extra/postgresql-42.5.4.jar /tmp/fix-admin-party.java
