package utils;

public class HtmlGenerator {

    private HtmlGenerator() {
    }

    public static byte[] httpsTunnelEstablished() {
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.1 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    public static byte[] createInterceptHtml(String interceptHost) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n");
        builder.append("<html>\n");
        builder.append("<head>\n");
        builder.append("<meta charset=\"utf-8\">\n");
        builder.append("<title>Warring</title>\n");
        builder.append("</head>\n");
        builder.append("<body>\n");
        builder.append("    <h1> host: ");
        builder.append(interceptHost);
        builder.append(" in the blacklist");
        builder.append("</h1>");
        builder.append("</body>\n");
        builder.append("</html>\n");
        return builder.toString().getBytes();
    }
}
