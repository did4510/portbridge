package com.darsh.portbridge;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleUPnP {
    private static final Logger LOGGER = PortBridge.LOGGER;
    private static final String UPNP_DISCOVERY = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n";
    private String controlURL;
    private String serviceType;
    private String lastLocation;

    public boolean isUPnPAvailable() {
        try {
            discoverGateway();
            return controlURL != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void discoverGateway() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(3000);

        byte[] sendData = UPNP_DISCOVERY.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
        socket.send(sendPacket);

        byte[] recvBuf = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);

        try {
            socket.receive(recvPacket);
            String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
            parseDiscoveryResponse(response);
        } finally {
            socket.close();
        }
    }

    private void parseDiscoveryResponse(String response) {
        Pattern locationPattern = Pattern.compile("LOCATION: (.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = locationPattern.matcher(response);
        if (matcher.find()) {
            String location = matcher.group(1).trim();
            this.lastLocation = location;
            try {
                fetchServiceDescription(location);
            } catch (Exception e) {
                LOGGER.debug("Failed to fetch service description", e);
            }
        }
    }

    private void fetchServiceDescription(String location) throws Exception {
        URL url = new URL(location);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xml.append(line);
            }
            parseServiceDescription(xml.toString());
        }
    }

    private void parseServiceDescription(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        NodeList serviceList = doc.getElementsByTagName("service");
        for (int i = 0; i < serviceList.getLength(); i++) {
            Element service = (Element) serviceList.item(i);
            String serviceType = getElementValue(service, "serviceType");
            if (serviceType != null && serviceType.contains("WANIPConnection") || serviceType.contains("WANPPPConnection")) {
                this.serviceType = serviceType;
                this.controlURL = getElementValue(service, "controlURL");
                break;
            }
        }
    }

    private String getElementValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    public boolean openPortTCP(int externalPort, int internalPort, String internalIP, String description, int leaseDuration) {
        if (controlURL == null) return false;

        String soapAction = "\"" + serviceType + "#AddPortMapping\"";
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:AddPortMapping xmlns:u=\"" + serviceType + "\"><NewRemoteHost></NewRemoteHost><NewExternalPort>" + externalPort + "</NewExternalPort><NewProtocol>TCP</NewProtocol><NewInternalPort>" + internalPort + "</NewInternalPort><NewInternalClient>" + internalIP + "</NewInternalClient><NewEnabled>1</NewEnabled><NewPortMappingDescription>" + description + "</NewPortMappingDescription><NewLeaseDuration>" + leaseDuration + "</NewLeaseDuration></u:AddPortMapping></s:Body></s:Envelope>";

        return sendSOAPRequest(soapAction, body);
    }

    public boolean closePortTCP(int externalPort) {
        if (controlURL == null) return false;

        String soapAction = "\"" + serviceType + "#DeletePortMapping\"";
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:DeletePortMapping xmlns:u=\"" + serviceType + "\"><NewRemoteHost></NewRemoteHost><NewExternalPort>" + externalPort + "</NewExternalPort><NewProtocol>TCP</NewProtocol></u:DeletePortMapping></s:Body></s:Envelope>";

        return sendSOAPRequest(soapAction, body);
    }

    public boolean isMappedTCP(int externalPort) {
        if (controlURL == null) return false;

        String soapAction = "\"" + serviceType + "#GetSpecificPortMappingEntry\"";
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:GetSpecificPortMappingEntry xmlns:u=\"" + serviceType + "\"><NewRemoteHost></NewRemoteHost><NewExternalPort>" + externalPort + "</NewExternalPort><NewProtocol>TCP</NewProtocol></u:GetSpecificPortMappingEntry></s:Body></s:Envelope>";

        return sendSOAPRequest(soapAction, body);
    }

    public String getExternalIP() {
        if (controlURL == null) return null;

        String soapAction = "\"" + serviceType + "#GetExternalIPAddress\"";
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:GetExternalIPAddress xmlns:u=\"" + serviceType + "\"></u:GetExternalIPAddress></s:Body></s:Envelope>";

        try {
            URL url = new URL(controlURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("SOAPAction", soapAction);
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // Parse the response for NewExternalIPAddress
                Pattern ipPattern = Pattern.compile("<NewExternalIPAddress>(.*?)</NewExternalIPAddress>");
                Matcher matcher = ipPattern.matcher(response.toString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get external IP", e);
        }
        return null;
    }

    // Expose discovery details for diagnostics
    public String getServiceType() {
        return serviceType;
    }

    public String getControlURL() {
        return controlURL;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    private boolean sendSOAPRequest(String soapAction, String body) {
        try {
            URL url = new URL(controlURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("SOAPAction", soapAction);
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            LOGGER.debug("SOAP request failed", e);
            return false;
        }
    }
}