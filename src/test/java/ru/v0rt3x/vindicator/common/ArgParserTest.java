package ru.v0rt3x.vindicator.common;

import org.junit.Assert;
import org.junit.Test;
import ru.v0rt3x.vindicator.TestBase;

public class ArgParserTest extends TestBase {

    @Test
    public void testNoArgs() {
        ArgParser.Args args = ArgParser.parse("command");

        Assert.assertEquals(args.cmd(), "command");
        Assert.assertEquals(args.args().size(), 0);
        Assert.assertEquals(args.kwargs().size(), 0);
    }

    @Test
    public void testSimpleArgs() {
        ArgParser.Args args = ArgParser.parse("command arg1 arg2");

        Assert.assertEquals(args.cmd(), "command");
        Assert.assertEquals(args.args().size(), 2);
        Assert.assertEquals(args.kwargs().size(), 0);

        Assert.assertEquals(args.args().get(0), "arg1");
        Assert.assertEquals(args.args(0), "arg1");

        Assert.assertEquals(args.args().get(1), "arg2");
        Assert.assertEquals(args.args(1), "arg2");
    }

    @Test
    public void testSimpleKwArgs() {
        ArgParser.Args args = ArgParser.parse("command --kwarg1 --kwarg2");

        Assert.assertEquals(args.cmd(), "command");
        Assert.assertEquals(args.args().size(), 0);
        Assert.assertEquals(args.kwargs().size(), 2);

        Assert.assertEquals(args.kwargs().containsKey("kwarg1"), true);
        Assert.assertEquals(args.kwargs("kwarg1", "noValue"), "");

        Assert.assertEquals(args.kwargs().containsKey("kwarg2"), true);
        Assert.assertEquals(args.kwargs("kwarg2", "noValue"), "");
    }

    @Test
    public void testKwArgsWithValues() {
        ArgParser.Args args = ArgParser.parse("command --kwarg1 value1 --kwarg2 value2");

        Assert.assertEquals(args.cmd(), "command");
        Assert.assertEquals(args.args().size(), 0);
        Assert.assertEquals(args.kwargs().size(), 2);

        Assert.assertEquals(args.kwargs().containsKey("kwarg1"), true);
        Assert.assertEquals(args.kwargs("kwarg1", "noValue"), "value1");

        Assert.assertEquals(args.kwargs().containsKey("kwarg2"), true);
        Assert.assertEquals(args.kwargs("kwarg2", "noValue"), "value2");
    }

    @Test
    public void testMixedArgs() {
        ArgParser.Args args = ArgParser.parse("command arg1 --kwarg1 value1 --kwarg2 value2 arg2");

        Assert.assertEquals(args.cmd(), "command");
        Assert.assertEquals(args.args().size(), 2);
        Assert.assertEquals(args.kwargs().size(), 2);

        Assert.assertEquals(args.kwargs().containsKey("kwarg1"), true);
        Assert.assertEquals(args.kwargs("kwarg1", "noValue"), "value1");

        Assert.assertEquals(args.kwargs().containsKey("kwarg2"), true);
        Assert.assertEquals(args.kwargs("kwarg2", "noValue"), "value2");

        Assert.assertEquals(args.args(0), "arg1");

        Assert.assertEquals(args.args(1), "arg2");
    }
}