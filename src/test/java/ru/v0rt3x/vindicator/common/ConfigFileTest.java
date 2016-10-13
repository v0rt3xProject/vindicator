package ru.v0rt3x.vindicator.common;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.v0rt3x.vindicator.TestBase;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ConfigFileTest extends TestBase {

    private File configFile;
    private ConfigFile config;

    @Before
    public void setUp() throws Exception {
        configFile = folder.newFile();

        FileWriter configFileWriter = new FileWriter(configFile);

        configFileWriter.write("parameterWithoutGroup = parameterValue\n");

        configFileWriter.write("[test.group]\n");

        configFileWriter.write("stringParameter = stringValue\n");
        configFileWriter.write("stringParameterWithSpaces = stringValue withSpaces\n");
        configFileWriter.write("stringParameterWithQuotes = 'string Value With Quotes'\n");
        configFileWriter.write("stringParameterWithDoubleQuotes = \"string Value With Double Quotes\"\n");
        configFileWriter.write("stringParameterWithOddQuotes = \"string Value With Odd Quotes'\"\n");
        configFileWriter.write("stringParameterWithComment = stringValue #withComment\n");

        configFileWriter.write("# commentedParameter = commentedValue\n");

        configFileWriter.write("intParameter = 12345\n");
        configFileWriter.write("intParameterWithTypeOverflow = 2147483648\n");
        configFileWriter.write("intParameterWithNonIntValue = notIntValue\n");

        configFileWriter.write("longParameter = 2147483648\n");

        configFileWriter.write("floatParameter = 2.147483648\n");

        configFileWriter.write("doubleParameter = 2.147483648\n");

        configFileWriter.write("booleanParameter = true\n");
        configFileWriter.write("booleanParameterCapitalized = True\n");
        configFileWriter.write("booleanParameterUppercase = TRUE\n");

        configFileWriter.write("listParameter = listValue1, listValue2\n");
        configFileWriter.write("listParameterWithSingleElement = listValue\n");

        configFileWriter.flush();
        configFileWriter.close();

        config = new ConfigFile(configFile);
    }

    @After
    public void tearDown() {
        configFile.delete();
    }

    @Test
    public void testParameterWithoutGroup() {
        Assert.assertEquals("parameterValue", config.getString("DEFAULT.parameterWithoutGroup", "noValue"));
    }

    @Test
    public void testStringParameter() {
        Assert.assertEquals("stringValue", config.getString("test.group.stringParameter", "noValue"));
    }

    @Test
    public void testStringWithSpaces() {
        Assert.assertEquals("stringValue withSpaces", config.getString("test.group.stringParameterWithSpaces", "noValue"));
    }

    @Test
    public void testStringWithQuotes() {
        Assert.assertEquals("'string Value With Quotes'", config.getString("test.group.stringParameterWithQuotes", "noValue"));
    }

    @Test
    public void testStringWithDoubleQuotes() {
        Assert.assertEquals("\"string Value With Double Quotes\"", config.getString("test.group.stringParameterWithDoubleQuotes", "noValue"));
    }

    @Test
    public void testStringWithOddQuotes() {
        Assert.assertEquals("\"string Value With Odd Quotes'\"", config.getString("test.group.stringParameterWithOddQuotes", "noValue"));
    }

    @Test
    public void testStringWithComment() {
        Assert.assertEquals("stringValue ", config.getString("test.group.stringParameterWithComment", "noValue"));
    }

    @Test
    public void testIntParameter() {
        Assert.assertEquals(Integer.valueOf(12345), config.getInt("test.group.intParameter", -1));
    }

    @Test
    public void testIntOverflow() {
        exception.expect(NumberFormatException.class);
        Assert.assertEquals(Integer.valueOf(-1), config.getInt("test.group.intParameterWithTypeOverflow", -1));
    }

    @Test
    public void testInvalidIntValue() {
        exception.expect(NumberFormatException.class);
        Assert.assertEquals(Integer.valueOf(-1), config.getInt("test.group.intParameterWithNonIntValue", -1));
    }

    @Test
    public void testLongParameter() {
        Assert.assertEquals(Long.valueOf(2147483648L), config.getLong("test.group.longParameter", -1L));
    }

    @Test
    public void testFloatParameter() {
        Assert.assertEquals(2.147483648f, config.getFloat("test.group.floatParameter", -1.0f));
    }

    @Test
    public void testDoubleParameter() {
        Assert.assertEquals(2.147483648, config.getDouble("test.group.doubleParameter", -1.0));
    }

    @Test
    public void testBooleanParameter() {
        Assert.assertEquals(Boolean.valueOf(true), config.getBoolean("test.group.booleanParameter", false));
    }

    @Test
    public void testBooleanCapitalized() {
        Assert.assertEquals(Boolean.valueOf(true), config.getBoolean("test.group.booleanParameterCapitalized", false));
    }

    @Test
    public void testBooleanUpperCase() {
        Assert.assertEquals(Boolean.valueOf(true), config.getBoolean("test.group.booleanParameterUppercase", false));
    }

    @Test
    public void testListParameter() {
        List<String> listValue = new ArrayList<>();

        listValue.add("listValue1");
        listValue.add("listValue2");

        Assert.assertEquals(listValue, config.getList("test.group.listParameter", String.class));
    }

    @Test
    public void testListWithSingleElement() {
        List<String> listValue = new ArrayList<>();

        listValue.add("listValue");

        Assert.assertEquals(listValue, config.getList("test.group.listParameterWithSingleElement", String.class));
    }
}
