import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Diagnostic: Try every known method to fetch users from ZKTeco device.
 * Mirrors exactly what Python pyzk does, with fallback to CMD 13.
 */
public class DiagUsers {
    static String IP = "10.2.1.100";
    static int PORT = 4370;
    static DatagramSocket sock;
    static int session = 0;
    static int replyId = 0;

    public static void main(String[] args) throws Exception {
        sock = new DatagramSocket();
        sock.setSoTimeout(5000);
        InetAddress addr = InetAddress.getByName(IP);

        // ── 1. Connect ──
        byte[] resp = sendRecv(addr, makePacket(1000, null));
        if (resp == null) { System.out.println("CONNECT FAILED"); return; }
        session  = getShort(resp, 4);
        replyId  = getShort(resp, 6);
        System.out.println("Connected. Session=" + session);

        // ── 2. Try CMD 1503 + FCT_USER payload (Python method) ──
        System.out.println("\n=== TEST 1: CMD 1503 + FCT_USER (Python pyzk method) ===");
        // pack('<bhii', 1, 9, 5, 0)
        byte[] py_payload = new byte[11];
        py_payload[0] = 1;
        putShort(py_payload, 1, 9);   // CMD_USERTEMP_RRQ
        putInt(py_payload, 3, 5);     // FCT_USER
        putInt(py_payload, 7, 0);
        resp = sendRecv(addr, makePacket(1503, py_payload));
        printResp("CMD1503+FCT_USER", resp);

        // ── 3. Try CMD 9 (CMD_USERTEMP_RRQ) directly ──
        System.out.println("\n=== TEST 2: CMD 9 direct (CMD_USERTEMP_RRQ) ===");
        resp = sendRecv(addr, makePacket(9, null));
        printResp("CMD9", resp);

        // ── 4. Try CMD 13 (READALLDATA) - the one that worked for attendance ──
        System.out.println("\n=== TEST 3: CMD 13 READALLDATA ===");
        resp = sendRecv(addr, makePacket(13, new byte[4]));
        printResp("CMD13", resp);
        if (resp != null) {
            int cmd = getShort(resp, 0);
            if (cmd == 1500) {
                int totalSize = getInt(resp, 8);
                System.out.println("  PREPARE_DATA totalSize=" + totalSize);
                // Read first chunk to see header
                byte[] chunk = readNextPacket();
                if (chunk != null) {
                    System.out.println("  First chunk CMD=" + getShort(chunk,0) + " len=" + chunk.length);
                    if (chunk.length >= 16) {
                        int h0 = getInt(chunk, 8);
                        int h4 = getInt(chunk, 12);
                        System.out.println("  Header[0]=" + h0 + " Header[4]=" + h4);
                        System.out.println("  => If 40-byte records: userCount=" + h0 + " (" + h0 + " users)");
                        System.out.println("  => If 28-byte records: userCount=" + (h0 / 28) + " users");
                    }
                }
            } else if (cmd == 2000) {
                // ACK_OK with inline data
                System.out.println("  ACK_OK with " + (resp.length - 8) + " inline bytes");
                if (resp.length >= 16) {
                    System.out.println("  Inline header[0]=" + getInt(resp,8) + " header[4]=" + getInt(resp,12));
                }
            }
        }

        // ── 5. Try CMD 50 (GET_FREE_SIZES) to check device user count ──
        System.out.println("\n=== TEST 4: CMD 50 GET_FREE_SIZES (device reports user count) ===");
        resp = sendRecv(addr, makePacket(50, null));
        printResp("CMD50", resp);
        if (resp != null && resp.length >= 92) {
            int users = getInt(resp, 24);
            int records = getInt(resp, 40);
            System.out.println("  Device reports: users=" + users + " attendance_records=" + records);
        }

        // Disconnect
        sendRecv(addr, makePacket(1001, null));
        sock.close();
    }

    static void printResp(String label, byte[] resp) {
        if (resp == null) { System.out.println("  " + label + " → TIMEOUT/NULL"); return; }
        System.out.println("  " + label + " → CMD=" + getShort(resp,0) + " len=" + resp.length);
        System.out.print("  HEX: ");
        for (int i = 0; i < Math.min(resp.length, 32); i++) System.out.printf("%02X ", resp[i]);
        System.out.println();
    }

    static byte[] readNextPacket() {
        try {
            byte[] buf = new byte[65535];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            sock.receive(p);
            byte[] r = new byte[p.getLength()];
            System.arraycopy(buf, 0, r, 0, p.getLength());
            return r;
        } catch (Exception e) { return null; }
    }

    static byte[] sendRecv(InetAddress addr, byte[] pkt) throws Exception {
        sock.send(new DatagramPacket(pkt, pkt.length, addr, PORT));
        return readNextPacket();
    }

    static byte[] makePacket(int cmd, byte[] data) {
        int dl = (data == null ? 0 : data.length);
        byte[] p = new byte[8 + dl];
        putShort(p, 0, cmd);
        putShort(p, 4, session);
        putShort(p, 6, ++replyId);
        if (data != null) System.arraycopy(data, 0, p, 8, dl);
        putShort(p, 2, checksum(p));
        return p;
    }

    static int checksum(byte[] b) {
        int c = 0;
        for (int i = 0; i + 1 < b.length; i += 2) {
            if (i == 2) continue;
            c += getShort(b, i);
            if (c > 0xFFFF) c -= 0xFFFF;
        }
        if (b.length % 2 != 0) { c += (b[b.length-1] & 0xFF); if (c > 0xFFFF) c -= 0xFFFF; }
        return (~c) & 0xFFFF;
    }

    static int getShort(byte[] b, int o) { return (b[o]&0xFF)|((b[o+1]&0xFF)<<8); }
    static int getInt(byte[] b, int o) { return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24); }
    static void putShort(byte[] b, int o, int v) { b[o]=(byte)(v&0xFF); b[o+1]=(byte)((v>>8)&0xFF); }
    static void putInt(byte[] b, int o, int v) { b[o]=(byte)(v&0xFF); b[o+1]=(byte)((v>>8)&0xFF); b[o+2]=(byte)((v>>16)&0xFF); b[o+3]=(byte)((v>>24)&0xFF); }
}
