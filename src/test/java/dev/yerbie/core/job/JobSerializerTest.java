package dev.yerbie.core.job;

import static org.junit.Assert.*;

import dev.yerbie.stub.StubData;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobSerializerTest {
  JobSerializer jobSerializer;

  @Before
  public void setUp() {
    jobSerializer = new JobSerializer(Jackson.newObjectMapper());
  }

  @Test
  public void testDeserializeJob() throws IOException {
    assertEquals(
        StubData.SAMPLE_JOB_DATA, jobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING));
  }

  @Test
  public void testSerializeJob() throws IOException {
    assertEquals(
        StubData.SAMPLE_JOB_DATA_STRING, jobSerializer.serializeJob(StubData.SAMPLE_JOB_DATA));
  }

  @Test
  public void testSerializeJobUnit() throws IOException {
    assertEquals(
        StubData.SAMPLE_JOB_UNIT_STRING, jobSerializer.serializeJobUnit(StubData.SAMPLE_JOB_UNIT));
  }

  @Test
  public void testDeserializeJobUnit() throws IOException {
    JobUnit deserializedJobUnit = jobSerializer.deserializeJobUnit(StubData.SAMPLE_JOB_UNIT_STRING);
    assertEquals(StubData.SAMPLE_JOB_UNIT, deserializedJobUnit);
    assertEquals(
        StubData.SAMPLE_JOB_DATA,
        jobSerializer.deserializeJob(deserializedJobUnit.getSerializedJobData().toString()));
  }
}
