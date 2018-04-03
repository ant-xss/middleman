package cn.org.hentai.client;

import cn.org.hentai.common.ByteUtils;
import cn.org.hentai.common.Configs;
import cn.org.hentai.common.Log;
import cn.org.hentai.common.Packet;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by matrixy on 2018/4/3.
 */
public class TranscodeSession extends Thread
{
    private Socket clientConnection = null;
    private Socket serverConnection = null;
    public TranscodeSession(Socket conn)
    {
        this.clientConnection = conn;
    }

    private void converse() throws Exception
    {
        int idleTimeout = Configs.getInt("socket.idletimeout", 60000);
        long lastActiveTime = System.currentTimeMillis();

        // 连接到服务器端
        serverConnection = new Socket(Configs.get("server.addr"), Configs.getInt("server.listen.port", 64035));
        clientConnection.setSoTimeout(Configs.getInt("socket.iotimeout", 30000));
        serverConnection.setSoTimeout(Configs.getInt("socket.iotimeout", 30000));
        InputStream clientIS = clientConnection.getInputStream(), serverIS = serverConnection.getInputStream();
        OutputStream clientOS = clientConnection.getOutputStream(), serverOS = serverConnection.getOutputStream();

        // 先发送校验包
        // 发送一个16字节的明文随机串，带上DES加密后的内容，用于验证
        serverOS.write(Packet.createAuthenticationPacket(Configs.get("server.authentication.password")));
        serverOS.flush();

        // 等待服务器端的响应，如果验证通过，则返回此次会话所使用的KEY
        byte[] resp = Packet.readEncryptedPacket(serverIS);
        // 前四字节：OJBK，后面16字节为本次会话的KEY
        if (resp[0] != 'O' || resp[1] != 'J' || resp[2] != 'B' || resp[3] != 'K') throw new RuntimeException("会话验证失败，请检查配置文件中的校验密码");
        if (resp.length != 20) throw new RuntimeException("错误的响应内容：" + ByteUtils.toString(resp));
        String key = new String(resp, 4, 16);
        Log.debug("本次会话密码：" + key);

        // 开始转发
        // 0xfa 0xfa 0xfa为协议头
        // 后四字节包长度
        // 再往后是加密包内容
    }

    @Override
    public void run()
    {
        try
        {
            converse();
        }
        catch(Exception e)
        {
            Log.error(e);
        }
    }
}
