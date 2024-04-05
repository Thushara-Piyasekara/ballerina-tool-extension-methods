package org.wso2.ballerina;

import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class BridgeCommandTest {
    @Test(description = "test bridge command with help flag")
    void testBridgeCommandWithHelpFlag() throws IOException {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        BridgeCommand bridgeCmd = new BridgeCommand(outputStream);
        String[] args = {"--help"};
        new CommandLine(bridgeCmd).parseArgs(args);
        bridgeCmd.execute();
        String scanLog = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        console.close();
        outputStream.close();
        String expected = "Tool for linking Ballerina compiler plugins\n";
        Assert.assertEquals(scanLog, expected);
    }
}
