# The Station's Discord Bot

[![Build Status](https://ci.bvereborn.com/buildStatus/icon?job=the-station-bot)](https://ci.bvereborn.com/job/the-station-bot)

A bot for a discord called the-station. https://discord.gg/tzP6UA3

## Build

```
sbt assembly
```

## Env vars

The bot requires a connection to a postgres database and a discord bot login tokin. The database must have schema that are discribed [here](https://github.com/cwfitzgerald/the-station-bot/blob/master/src/main/scala/com/cwfitz/the_station_bot/database/DBWrapper.scala#L7). They should be set in the appropriate env vars.

`CWF_USE_SSH` equal to `1` if you want the bot to connect using ssh.  
`CWF_HOST` is the host to connect to with ssh.  
`CWF_USER` is the username for the postgres db.  
`CWF_PASS` is the password for the postgres db.  
`API_KEY` is the discord api key.  

## Run

```
sbt run
```

or 

```
java -jar target/scala-2.12/the-station-bot.jar
```


