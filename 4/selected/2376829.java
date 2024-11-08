package net.sf.alc.connection.tcp;

import java.io.IOException;
import java.io.InterruptedIOException;
import net.sf.alc.connection.Connection;
import net.sf.alc.connection.ConnectionException;
import net.sf.alc.connection.ConnectionFactory;
import net.sf.alc.connection.tcp.protocolencoder.ProtocolEncoderFactory;

/**
 * @author alain.caron
 */
public class TcpConnectionFactory implements ConnectionFactory<TcpConnectionContext> {

    private final ProtocolDecoderFactory mDecoderFactory;

    private final ProtocolEncoderFactory mEncoderFactory;

    private final SocketConfigurator mConfigurator = new SocketConfigurator();

    public TcpConnectionFactory(ProtocolDecoderFactory aDecoderFactory, ProtocolEncoderFactory aEncoderFactory) {
        mDecoderFactory = aDecoderFactory;
        mEncoderFactory = aEncoderFactory;
    }

    public Connection createConnection(TcpConnectionContext aConnectionContext) throws ConnectionException {
        try {
            mConfigurator.configureSocket(aConnectionContext.getChannel().socket());
            return new TcpConnection(aConnectionContext, mDecoderFactory, mEncoderFactory);
        } catch (IOException e) {
            if (e instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            throw new ConnectionException(e);
        }
    }
}
