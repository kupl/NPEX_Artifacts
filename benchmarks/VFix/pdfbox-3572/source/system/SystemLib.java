package system;

import java.io.IOException;

public class SystemLib {
	public static void write(byte b[]) throws IOException {
        System.out.println(b+" length:"+b.length);
  }
}
