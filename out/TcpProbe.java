import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.tcp.TcpChatClient;

public class TcpProbe {
    public static void main(String[] args) throws Exception {
        TcpChatClient client = new TcpChatClient("127.0.0.1", 61000);
        client.connect();
        String u = "probe" + System.currentTimeMillis();
        AuthResponse auth = client.register(u, "1234", "Probe");
        for (int i = 0; i < 6; i++) {
            client.heartbeat(auth.sessionToken);
            System.out.println("heartbeat ok #" + (i + 1));
            Thread.sleep(1200);
        }
        client.close();
    }
}
