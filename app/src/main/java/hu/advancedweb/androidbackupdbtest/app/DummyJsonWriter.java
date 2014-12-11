package hu.advancedweb.androidbackupdbtest.app;

import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.output.NullWriter;

import java.io.IOException;

/**
 * Created by sashee on 12/7/14.
 */
public class DummyJsonWriter extends JsonWriter {
	public DummyJsonWriter(){
		super(new NullWriter());
	}

	@Override
	public JsonWriter name(String name) throws IOException {
		return this;
	}

	@Override
	public JsonWriter beginArray() throws IOException {
		return this;
	}

	@Override
	public JsonWriter beginObject() throws IOException {
		return this;
	}

	@Override
	public JsonWriter endArray() throws IOException {
		return this;
	}

	@Override
	public JsonWriter endObject() throws IOException {
		return this;
	}

	@Override
	public JsonWriter value(String value) throws IOException {
		return this;
	}

	@Override
	public JsonWriter value(boolean value) throws IOException {
		return this;
	}

	@Override
	public JsonWriter value(double value) throws IOException {
		return this;
	}

	@Override
	public JsonWriter value(long value) throws IOException {
		return this;
	}

	@Override
	public JsonWriter value(Number value) throws IOException {
		return this;
	}

	@Override
	public JsonWriter nullValue() throws IOException {
		return this;
	}

	@Override
	public void close() throws IOException {

	}
}
