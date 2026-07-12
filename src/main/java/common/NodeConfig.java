package common;

import java.nio.file.Path;

public record NodeConfig(
    Node node,
    int udpPort,
    Path storageDirectory
) {}