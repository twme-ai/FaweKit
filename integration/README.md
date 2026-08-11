# Mineflayer integration test

This test exercises FaweKit through the Minecraft protocol rather than
calling plugin classes directly.

1. Start Paper 1.21.11 with FAWE 2.15.x and the built plugin JAR.
2. Use an isolated test world, set `online-mode=false`, and grant operator status
   to the offline UUID for `FawesuiteTest`.
3. Run `npm ci && npm test` in this directory.

Set `MC_HOST` and `MC_PORT` when the server is not at `127.0.0.1:25565`. The
script edits coordinates around `(0,100,0)` through `(61,100,0)` and should not
be pointed at a production world.
