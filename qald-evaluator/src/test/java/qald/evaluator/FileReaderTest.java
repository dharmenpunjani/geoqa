package qald.evaluator;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;

public class FileReaderTest {

	@Ignore
	@Test
	public void test() throws UnsupportedEncodingException, IOException {

		FileReader filereader = new FileReader("/tmp/qald-gd-v0.1.0.json");

		fail("Not yet implemented");
	}

}
