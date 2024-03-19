package xeed.mc.paper.streamotes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class PacketUtils {
    private static void varIntsWrite(ByteBuf buf, int i) {
        while ((i & -128) != 0) {
            buf.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        buf.writeByte(i);
    }

    public static void writeString(ByteBuf buf, CharSequence string, int maxLength) {
        if (string.length() > maxLength) {
            throw new RuntimeException("String too big (was " + string.length() + " characters, max " + maxLength + ")");
        } else {
            int i = ByteBufUtil.utf8MaxBytes(string);
            var byteBuf = buf.alloc().buffer(i);

            try {
                int j = ByteBufUtil.writeUtf8(byteBuf, string);
                int k = ByteBufUtil.utf8MaxBytes(maxLength);
                if (j > k) {
                    throw new RuntimeException("String too big (was " + j + " bytes encoded, max " + k + ")");
                }

                varIntsWrite(buf, j);
                buf.writeBytes(byteBuf);
            } finally {
                byteBuf.release();
            }
        }
    }
}
