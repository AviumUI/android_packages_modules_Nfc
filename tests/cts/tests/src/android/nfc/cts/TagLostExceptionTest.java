package android.nfc.cts;

import static org.junit.Assert.assertTrue;

import android.nfc.TagLostException;
import org.junit.Assert;
import org.junit.Test;

public class TagLostExceptionTest {

  @Test
  public void testTagLostException() {
    try {
      throw new TagLostException();
    } catch (TagLostException e) {
      assertTrue("TagLostException message is null", e.getMessage() == null);
    }
    String s = new String("testTagLostException");
    try {
      throw new TagLostException(s);
    } catch (TagLostException e) {
      assertTrue("TagLostException message is not correct", e.getMessage().equals(s));
    }
  }
}
