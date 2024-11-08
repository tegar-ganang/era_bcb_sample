package model.device.input;

import java.io.*;
import java.net.*;
import model.util.Channel;

public class NetworkServer implements IInputDevice {

    private InputDeviceConnector idc;

    private ServerSocket server;

    private Socket s;

    private Thread serverThread;

    private volatile Boolean toContinue = true;

    public NetworkServer(InputDeviceConnector _idc) {
        this.idc = _idc;
        this.startListening();
    }

    ;

    public void startListening() {
        System.out.println("starting the network");
        serverThread = new Thread() {

            public void run() {
                try {
                    synchronized (toContinue) {
                        toContinue = true;
                    }
                    server = new ServerSocket(17853);
                    System.out.println("Waiting for connection");
                    while (toContinue) {
                        try {
                            s = server.accept();
                        } catch (SocketException se) {
                            return;
                        }
                        System.out.println("Connected, waiting to receive");
                        BufferedInputStream in = new BufferedInputStream(s.getInputStream());
                        OutputStream out = s.getOutputStream();
                        while (s.isConnected()) {
                            if (!toContinue) break;
                            if (in.available() > 0) {
                                try {
                                    short header = (short) in.read();
                                    if (header == (short) 1) {
                                        int address = in.read();
                                        int special = in.read();
                                        int ichannel = ((special << 1) & 0x00000100) | address;
                                        int value = in.read();
                                        if ((special & 0x00000001) == 1) value = -100;
                                        System.out.println("Updating chan=" + ichannel + " val=" + value);
                                        Channel channel = new Channel((short) ichannel, (short) value);
                                        idc.updateChannel(channel);
                                    } else if (header == (short) 2) {
                                        idc.goToNextCue();
                                    } else if (header == (short) 3) {
                                    } else if (header == (short) 4) {
                                    } else if (header == (short) 5) {
                                        idc.resetAllChannels();
                                    } else if (header == (short) 6) {
                                        Channel[] channels = idc.getChannels();
                                        sendChannels(channels, idc.getChannelSources(channels), (byte) 6, out);
                                    } else if (header == (short) 7) {
                                        Channel[] channels = idc.getInputDeviceChannels();
                                        sendChannels(channels, idc.getChannelSources(channels), (byte) 7, out);
                                    } else if (header == (short) 255) {
                                        out.write((byte) 0xFF);
                                        in.close();
                                        out.close();
                                        s.close();
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Thread.yield();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        serverThread.start();
    }

    private void sendChannels(Channel[] channels, float[] sources, byte header, OutputStream output) {
        byte[] data = new byte[5 * channels.length];
        int startPoint = 0;
        for (int i = 0; i < channels.length; i++) {
            data[i * 5 + startPoint] = header;
            data[i * 5 + 1 + startPoint] = (byte) (0x00FF & channels[i].address);
            data[i * 5 + 2 + startPoint] = (byte) ((channels[i].address & 0x0100) >> 1);
            if (channels[i].value < 0) {
                data[i * 5 + 2 + startPoint] |= 0x01;
                data[i * 5 + 3 + startPoint] = 0;
            } else data[i * 5 + 3 + startPoint] = (byte) channels[i].value;
            System.out.println("For i=" + i + " source[i]=" + sources[i]);
            if (sources[i] < 0) data[i * 5 + 4 + startPoint] = (byte) (-1 * sources[i]); else data[i * 5 + 4 + startPoint] = (byte) 0xFF;
        }
        try {
            System.out.println("Writing output data of length=" + data.length);
            output.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        System.out.println("Stopping the network server");
        try {
            server.close();
            synchronized (toContinue) {
                toContinue = false;
            }
            System.out.println("Closed the socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
