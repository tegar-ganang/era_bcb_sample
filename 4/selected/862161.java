package net.sf.cotta.acceptance;

import net.sf.cotta.*;
import net.sf.cotta.io.InputManager;
import net.sf.cotta.io.InputProcessor;
import net.sf.cotta.memory.AccesssUtil;
import org.jbehave.core.Block;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

public abstract class InputFileChannelBehaviourBase extends UsingTfsConstratins {

    public void setUp() throws Exception {
        super.setUp();
        file().delete();
    }

    public void tearDown() throws TIoException {
        file().delete();
        super.tearDown();
    }

    public void shouldSupportFileChannel() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/source/file.txt");
        file.save("test");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                InputFileChannelBehaviourBase.this.process(inputManager.channel());
            }
        });
        FileChannel channel = file.io().inputChannel();
        process(channel);
        channel.close();
    }

    public void shouldHandleReadWhenPositionedAtEndOfFile() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/test.txt");
        file.save("content");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                ByteBuffer buffer = ByteBuffer.allocate(3);
                FileChannel channel = inputManager.channel();
                assertThat().integer(channel.read(buffer)).isEqualTo(3);
                assertThat().object(channel.position(channel.size())).isSameAs(channel);
                assertThat().integer(channel.read(buffer)).isEqualTo(0);
                buffer.clear();
                assertThat().integer(channel.read(buffer)).isEqualTo(-1);
            }
        });
    }

    public void shouldNotAllowWriteOperations() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/test.txt");
        file.save("content");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                final FileChannel channel = inputManager.channel();
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.write(ByteBuffer.allocate(3));
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.write(new ByteBuffer[] { ByteBuffer.allocate(3) }, 0, 1);
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.write(new ByteBuffer[] { ByteBuffer.allocate(3) });
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.write(ByteBuffer.allocate(3), 0);
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.truncate(3);
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.transferFrom(AccesssUtil.createInMemoryOutputChannel(), 0, 1);
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.map(FileChannel.MapMode.READ_WRITE, 0, 10);
                    }
                });
                assertNotSupported(new Block() {

                    public void run() throws Exception {
                        channel.map(FileChannel.MapMode.PRIVATE, 0, 10);
                    }
                });
            }
        });
    }

    private void assertNotSupported(Block block) {
        Exception exception;
        try {
            exception = runAndCatch(NonWritableChannelException.class, block);
            assertThat().object(exception).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private TFileFactory factory() {
        return new TFileFactory(fileSystem());
    }

    protected abstract FileSystem fileSystem();

    public void shouldHandleCaseWhenBufferIsNotBigEnough() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/dir/test.txt");
        file.save("this is a very long content, well, sort of");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                ByteBuffer buffer = ByteBuffer.allocate(5);
                FileChannel channel = inputManager.channel();
                assertThat().integer(channel.read(buffer)).isEqualTo(5);
                assertThat().string(buffer.array()).isEqualTo("this ");
                assertThat().longValue(channel.position()).isEqualTo(5);
                assertThat().integer(channel.read(buffer)).isEqualTo(0);
                assertThat().string(buffer.array()).isEqualTo("this ");
                assertThat().longValue(channel.position()).isEqualTo(5);
            }
        });
    }

    public void shouldPositionToAnyPoint() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/dir/test.txt");
        file.save("testing re-positioning of the channel");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                ByteBuffer buffer = ByteBuffer.allocate(10);
                FileChannel channel = inputManager.channel();
                assertThat().integer(channel.read(buffer)).isEqualTo(10);
                assertThat().string(buffer.array()).isEqualTo("testing re");
                buffer.clear();
                channel.position(1);
                assertThat().longValue(channel.position()).isEqualTo(1);
                assertThat().integer(channel.read(buffer)).isEqualTo(10);
                assertThat().string(buffer.array()).isEqualTo("esting re-");
            }
        });
    }

    public void shouldAllowsPositionToPassEndOfFile() throws Exception {
        TFileFactory factory = factory();
        TFile file = factory.file("tmp/dir/test.txt");
        file.save("testing positioning to end of file");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                ByteBuffer buffer = ByteBuffer.allocate(3);
                FileChannel channel = inputManager.channel();
                assertThat().object(channel.position(channel.size() + 3)).isSameAs(channel);
                assertThat().longValue(channel.position()).isEqualTo(channel.size() + 3);
                assertThat().integer(channel.read(buffer)).isEqualTo(-1);
            }
        });
    }

    private void process(FileChannel channel) throws IOException {
        assertThat().longValue(channel.position()).isEqualTo(0);
        assertThat().longValue(channel.size()).isEqualTo(4);
        ByteBuffer buffer = ByteBuffer.allocate(5);
        int count = channel.read(buffer);
        assertThat().integer(count).isEqualTo(4);
        byte[] bytes = buffer.array();
        String actual = new String(bytes, 0, buffer.position());
        assertThat().string(actual).isEqualTo("test");
    }

    public void shouldNotFailForce() throws Exception {
        TFile file = factory().file("tmp/content2.txt").save("test");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                inputManager.channel().force(true);
            }
        });
    }

    public void shouldTransferToTarget() throws Exception {
        final TFileFactory factory = factory();
        TFile file = factory.file("tmp/content1.txt").save("content");
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                FileChannel outputChannel = AccesssUtil.createInMemoryOutputChannel();
                FileChannel inputChannel = inputManager.channel();
                long transferred = inputChannel.transferTo(1, 3, outputChannel);
                assertThat().longValue(transferred).isEqualTo(3);
                assertThat().inMemoryOutput(outputChannel).hasContent("ont");
                assertThat().longValue(inputChannel.position()).isEqualTo(0);
            }
        });
    }

    public void shouldTransferOnlyAvailableBytes() throws Exception {
        TFile file = file();
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                FileChannel outputChannel = AccesssUtil.createInMemoryOutputChannel();
                FileChannel inChannel = inputManager.channel();
                long transferred = inChannel.transferTo(5, 10, outputChannel);
                assertThat().longValue(transferred).isEqualTo(5);
                assertThat().inMemoryOutput(outputChannel).hasContent("67890");
                assertThat().longValue(inChannel.position()).isEqualTo(0);
                assertThat().longValue(inChannel.transferTo(100, 1, outputChannel)).isEqualTo(0);
                outputChannel.close();
            }
        });
    }

    protected TFile file() throws TIoException {
        final TFileFactory factory = factory();
        return factory.file("tmp/content.txt").save("1234567890");
    }

    public void shouldReadToByteBufferFromAnyPosition() throws Exception {
        TFile file = file();
        final ByteBuffer buffer = ByteBuffer.allocate(6);
        file.read(new InputProcessor() {

            public void process(InputManager inputManager) throws IOException {
                FileChannel channel = inputManager.channel();
                int read = channel.read(buffer, 3);
                assertThat().integer(read).isEqualTo(6);
                assertThat().string(buffer.array()).isEqualTo("456789");
                assertThat().longValue(channel.position()).isEqualTo(0);
            }
        });
    }
}
